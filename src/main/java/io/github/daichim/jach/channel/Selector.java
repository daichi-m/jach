package io.github.daichim.jach.channel;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import io.github.daichim.jach.exception.TooManySelectorException;
import io.github.daichim.jach.internal.AfterWriteAction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * {@link Selector} is used to run a select call over multiple {@link Channel}s and take action
 * based on which channel a message appears (through a set of {@link ChannelAction} objects).
 * <p>
 * The standard way of creating a {@link Selector} is to use {@link Selector#of(ChannelAction[])}.
 * It can provide a similar interface as select-case construct in golang. A typical instantiation of
 * a {@link Selector} would look like:
 *
 * <pre>
 *     Selector selector = Selector.of(
 *        ChannelAction.action(channel, msg -&#62; {
 *            // Do something awesome.
 *        },
 *        ChannelAction.action(channel1, msg1 -&#62; {
 *            // Do something else, still being awesome
 *        },
 *        ChannelAction.action(exitChan, exitMsg -&#62; {
 *            Selector.BREAK_ACTION;
 *        });
 * </pre>
 * <p>
 * After the Selector is created, then it can be used in three different methods.
 * <ul>
 * <li> It can be used for a one-time select using {@link #select()}.</li>
 * <li> It can be used as a looping construct using {@link #untilDone()}.</li>
 * <li> It can also be used as a looping construct with a default case using
 * {@link #untilOrDefault(Action)}.</li>
 * </ul>
 * <p>
 * Two predefined {@link Consumer} instances are present in this class, BREAK_ACTION and
 * CONTINUE_ACTION. The objects can be used in the looping methods as a drop-in replacement for
 * break and continue respectively.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Slf4j
public class Selector implements AutoCloseable {

    /**
     * {@link Consumer} instance that can be used to indicate to break from the loop in case of
     * {@link #untilDone()} or {@link #untilOrDefault(Action)} methods.
     */
    public static final Consumer BREAK_ACTION = (x) -> {};

    /**
     * {@link Consumer} instance that can be used to skip and continue to the next iteration of the
     * loop in case of {@link #untilDone()} or {@link #untilOrDefault(Action)} methods.
     */
    public static final Consumer CONTINUE_ACTION = (x) -> {};

    private static final int CHAN_SIZE = 2048;
    private static final String CLOSE_CHAN_PREFIX = "__CLOSE__";
    private static final String EMPTY = "";
    private final Queue<String> backfillQueue;
    private final Set<String> cleanupSet;
    private Map<String, ChannelAction> channelActions;
    private Channel<String> selectorChannel;

    @Getter
    private volatile boolean active;

    @VisibleForTesting
    Selector() {
        this.backfillQueue = new LinkedBlockingQueue<>();
        this.cleanupSet = Collections.synchronizedSet(new HashSet<String>());
        this.active = true;
    }

    /**
     * Creates a new {@link Selector} from the given set of {@link ChannelAction}s.
     *
     * @param actions The list of {@link ChannelAction}s that this {@link Selector} would select
     *                from.
     *
     * @return A new {@link Selector} object.
     */
    public static Selector of(ChannelAction... actions)
        throws TooManySelectorException, NullPointerException {

        Selector selector = new Selector();
        selector.channelActions = new HashMap<>(actions.length);
        selector.selectorChannel = new BufferedChannel<>(CHAN_SIZE, String.class);

        for (ChannelAction ca : actions) {
            Preconditions.checkNotNull(ca.getChannel());
            Preconditions.checkNotNull(ca.getAction());
            Preconditions.checkState(ca.getChannel().isOpen());
            selector.channelActions.put(ca.getChannel().getId(), ca);
            ca.getChannel().registerAfterWriteAction(selector.createAFW(ca));
        }
        return selector;
    }

    private AfterWriteAction createAFW(ChannelAction ca) {
        return new AfterWriteAction() {

            @Override
            public void onWrite() {
                String id = ca.getChannel().getId();
                boolean success = selectorChannel.tryWrite(id);
                if (!success) {
                    selectorChannel.tryRead();
                    success = selectorChannel.tryWrite(id);
                    if (!success) {
                        log.warn("Unable to write to selector queue. Selectors won't work");
                    }
                    log.warn("Messages getting dropped since no thread is reading selector");
                }
            }

            @Override
            public void close() {
                String id = ca.getChannel().getId();
                boolean success = selectorChannel.tryWrite(CLOSE_CHAN_PREFIX + id);
                log.debug("Close message for channel {} written", id);
                if (!success) {
                    selectorChannel.tryRead();
                    success = selectorChannel.tryWrite(CLOSE_CHAN_PREFIX + id);
                    if (!success) {
                        log.warn("Unable to write to selector queue. Selectors won't work");
                    }
                    log.warn("Messages getting dropped since no thread is reading selector");
                }
            }
        };
    }


    /**
     * Waits for a message to be recieved on any of the channels associated with the {@link
     * ChannelAction}s of this {@link Selector} and execute the action corresponding to that
     * channel.
     *
     * @throws IllegalStateException If there is an issue with the {@link Selector} (selector's
     *                               internal channel is closed, or there is a null channel or
     *                               action registered with the selector). Generally these scenarios
     *                               should not occur.
     */
    public void select() throws IllegalStateException {
        try {
            if (!this.isActive()) {
                throw new IllegalStateException("Selector is closed");
            }
            String chanId = this.selectorChannel.read();
            if (chanId.startsWith(CLOSE_CHAN_PREFIX)) {
                closeChannel(chanId.replace(CLOSE_CHAN_PREFIX, EMPTY));
                return;
            }

            ChannelAction chan = this.channelActions.get(chanId);
            Preconditions.checkNotNull(chan);
            Preconditions.checkNotNull(chan.getChannel());
            Preconditions.checkNotNull(chan.getAction());

            Object msg = chan.getChannel().tryRead();
            if (msg == null) {
                // Some other thread has already read in the message. Let's go into select again.
                this.select();
            }
            if (chan.getAction() == BREAK_ACTION) {
                return;
            } else if (chan.getAction() == CONTINUE_ACTION) {
                this.select();
            } else {
                chan.getAction().accept(msg);
            }
        } catch (ClosedChannelException |
            NoSuchChannelElementException |
            NullPointerException ex) {

            throw new IllegalStateException(ex);
        }
    }

    /**
     * Runs an loop over all the channels and executes the action associated with that channel as
     * and when a message is received on that channel. The loop breaks when all the channels are
     * closed or BREAK_ACTION is called on receiving message on any channel.
     *
     * @throws IllegalStateException In case of an issue with the internal state.
     */
    public void untilDone() throws IllegalStateException {

        if (!this.isActive()) {
            throw new IllegalStateException("Selector is closed");
        }

        while (this.isActive()) {
            try {
                String chanId = this.selectorChannel.read();
                if (chanId.startsWith(CLOSE_CHAN_PREFIX)) {
                    closeChannel(chanId.replace(CLOSE_CHAN_PREFIX, EMPTY));
                    continue;
                }
                ChannelAction ca = this.channelActions.get(chanId);
                Preconditions.checkNotNull(ca);
                Preconditions.checkNotNull(ca.getChannel());
                Preconditions.checkNotNull(ca.getAction());

                Object msg = ca.getChannel().tryRead();
                if (msg == null) {
                    continue;
                } else if (ca.getAction() == BREAK_ACTION) {
                    this.close();
                    break;
                } else if (ca.getAction() == CONTINUE_ACTION) {
                    continue;
                } else {
                    ca.getAction().accept(msg);
                }
            } catch (NullPointerException ex) {
                throw new IllegalStateException(ex);
            } catch (NoSuchChannelElementException | ClosedChannelException ex) {
                log.warn("Unexpected channel exception - {}", ex.getMessage());
            }
        }
    }

    /**
     * Runs an loop over all the channels and executes the action associated with that channel as
     * and when a message is received on that channel. When no message is present on any channel the
     * defaultAction is executed. The loop breaks when all the channels are closed or BREAK_ACTION
     * is called on receiving message on any channel.
     *
     * @param defaultAction The default action to run when there is no message in any of the
     *                      channels associated with this selector.
     *
     * @throws IllegalStateException If the internal state has issues.
     */
    public void untilOrDefault(Action defaultAction) throws IllegalStateException {

        if (!this.isActive()) {
            throw new IllegalStateException("Selector is closed");
        }

        while (this.isActive()) {
            try {
                String chanId = this.selectorChannel.tryRead();
                if (chanId == null) {
                    defaultAction.accept(null);
                    continue;
                }
                if (chanId.startsWith(CLOSE_CHAN_PREFIX)) {
                    closeChannel(chanId.replace(CLOSE_CHAN_PREFIX, EMPTY));
                    continue;
                }
                ChannelAction ca = this.channelActions.get(chanId);
                Preconditions.checkNotNull(ca);
                Preconditions.checkNotNull(ca.getChannel());
                Preconditions.checkNotNull(ca.getAction());

                Object msg = ca.getChannel().tryRead();
                if (msg == null) {
                    defaultAction.accept(null);
                    continue;
                } else if (ca.getAction() == BREAK_ACTION) {
                    this.close();
                    break;
                } else if (ca.getAction() == CONTINUE_ACTION) {
                    continue;
                } else {
                    ca.getAction().accept(msg);
                }
            } catch (NullPointerException ex) {
                throw new IllegalStateException(ex);
            } catch (NoSuchChannelElementException | ClosedChannelException ex) {
                log.warn("Unexpected channel exception - {}", ex.getMessage());
            }
        }
    }

    private void closeChannel(String channel) {
        this.channelActions.remove(channel);
        if (this.channelActions.isEmpty()) {
            close();
        }
        log.debug("Channel removed: {}", channel);
    }

    /**
     * Close the {@link Selector}.
     */
    @Override
    public void close() {
        this.active = false;
    }
}
