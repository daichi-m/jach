package io.github.daichim.jach.channel;

import com.google.common.annotations.VisibleForTesting;
import io.github.daichim.jach.exception.TooManySelectorException;

import java.util.HashMap;
import java.util.Map;
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
 *        ChannelAction.actOn(channel, msg -&#62; {
 *            // Do something awesome.
 *        },
 *        ChannelAction.actOn(channel1, msg1 -&#62; {
 *            // Do something else, still being awesome
 *        },
 *        ChannelAction.actOn(exitChan, exitMsg -&#62; {
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
@SuppressWarnings("rawtypes")
public class Selector {

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

    private Map<String, ChannelAction> channelActions;
    private Channel<String> selectorChannel;

    @VisibleForTesting
    Selector() {
    }

    /**
     * Creates a new {@link Selector} from the given set of {@link ChannelAction}s.
     *
     * @param actions The list of {@link ChannelAction}s that this {@link Selector} would select
     *                from.
     *
     * @return A new {@link Selector} object.
     */
    public static Selector of(ChannelAction... actions) throws TooManySelectorException {
        Selector selector = new Selector();
        selector.channelActions = new HashMap<>(actions.length);
        selector.selectorChannel = new BufferedChannel<>(CHAN_SIZE, String.class);
        try {
            for (ChannelAction ca : actions) {
                selector.channelActions.put(ca.getChannel().getId(), ca);
                ca.getChannel().registerAfterWriteAction(() -> {});
            }
        } catch (IllegalStateException ex) {
            throw new TooManySelectorException(ex.getMessage(), ex);
        }
        return selector;
    }


    /**
     * Waits for a message to be recieved on any of the channels associated with the {@link
     * ChannelAction}s of this {@link Selector} and execute the action corresponding to that
     * channel.
     */
    public void select() {

    }

    /**
     * Runs an loop over all the channels and executes the action associated with that channel as
     * and when a message is received on that channel. The loop breaks when all the channels are
     * closed or BREAK_ACTION is called on receiving message on any channel.
     */
    public void untilDone() {

    }

    /**
     * Runs an loop over all the channels and executes the action associated with that channel as
     * and when a message is received on that channel. When no message is present on any channel the
     * defaultAction is executed. The loop breaks when all the channels are closed or BREAK_ACTION
     * is called on receiving message on any channel.
     *
     * @param defaultAction The default action to run when there is no message in any of the
     *                      channels associated with this selector.
     */
    public void untilOrDefault(Action defaultAction) {

    }

}
