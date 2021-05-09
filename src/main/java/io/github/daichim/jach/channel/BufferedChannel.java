package io.github.daichim.jach.channel;

import com.google.common.base.Preconditions;
import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import io.github.daichim.jach.exception.TimeoutException;
import io.github.daichim.jach.exception.TooManySelectorException;
import io.github.daichim.jach.internal.AfterWriteAction;
import io.github.daichim.jach.internal.ChannelIterator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * {@link BufferedChannel} is an implementation of a {@link Channel} that has a fixed size buffer
 * backing it. Once the buffer is full subsequent attempts to write to it will block until there is
 * free space available in the buffer.
 * <p>
 * {@link BufferedChannel} is an {@link Iterable} and thus can be used in a for-each loop. The
 * {@link Iterator} for this channel throws a {@link NoSuchChannelElementException} when the channel
 * is closed, so the caller has to handle the exception on their end and perform any cleanup tasks
 * needed after the channel closure.
 * <p>
 * A typical for-each use-case would look like this:
 * <pre>
 *     // channel is a BufferedChannel&#60;String&#62; type.
 *     try {
 *         for (String msg : channel) {
 *             // do something with msg
 *         }
 *     } catch(NoSuchChannelElementException | ClosedChannelException ex) {
 *         // handle channel closure
 *     }
 * </pre>
 * <p>
 * A more streamlined format is to utilize the {@link #forEach(Consumer)} method, which will handle
 * channel closures gracefully. This method does not propagate the {@link
 * NoSuchChannelElementException} to the caller and the method only returns to the caller when the
 * channel is closed.
 *
 * @param <T> The type of the message which the {@link BufferedChannel} holds.
 */

@Slf4j
public class BufferedChannel<T> implements Channel<T> {

    private final BlockingQueue<T> internalQueue;
    private final int capacity;
    private final Class<T> clazz;
    private final String channelId;
    private final Map<Long, Thread> blockedWriters;
    private final Map<Long, Thread> blockedReaders;
    private final List<AfterWriteAction> afterWriteActionList;
    private final ChannelIterator<T> iterator;
    // This is a empirical figure.
    private final int MAX_AFTER_WRITE_ACTIONS = 25;
    private volatile boolean open;


    public BufferedChannel(int capacity, Class<T> clazz) {
        this.clazz = clazz;
        this.capacity = capacity;
        this.internalQueue = new ArrayBlockingQueue<>(capacity, true);
        this.channelId = UUID.randomUUID().toString();
        this.open = true;

        this.blockedReaders = Collections.synchronizedMap(new HashMap<>());
        this.blockedWriters = Collections.synchronizedMap(new HashMap<>());
        this.afterWriteActionList = Collections.synchronizedList(new ArrayList<>());
        this.iterator = new ChannelIterator<>(this);

    }

    /**
     * Writes a non-null message to the channel. If the channel does not have enough space to write
     * the message, the thread is blocked until space is available.
     *
     * @throws ClosedChannelException In case the channel has already been closed, or got closed
     *                                before write could succeed.
     * @throws IllegalStateException  In case of an unexpected error in writing the message to the
     *                                channel.
     * @throws NullPointerException   If the msg is {@literal null}.
     * @see Channel#write(Object)
     */
    @Override
    public void write(T message) throws ClosedChannelException, IllegalStateException {
        try {
            blockedWrite(message, Optional.empty(), Optional.empty());
        } catch (TimeoutException ignored) {
        }
    }

    /**
     * Tries writing a message to the {@link BufferedChannel}, blocking if space is not available
     * for a maximum of the timeout period.
     *
     * @param message The message to write to the {@link BufferedChannel}.
     * @param timeout The timeout value after which the write times out.
     * @param unit    The unit of the timeout value.
     *
     * @throws TimeoutException       If the write times out after the timeout period.
     * @throws ClosedChannelException If the {@link BufferedChannel} has already been closed for
     *                                writing.
     */
    @Override
    public void write(T message, int timeout, TimeUnit unit) throws TimeoutException {
        blockedWrite(message, Optional.of(timeout), Optional.ofNullable(unit));
    }

    /**
     * Tries writing a message to the {@link BufferedChannel}. If the write is successful, returns
     * {@literal true}, if the write fails due to lack of space, returns {@literal false}.
     *
     * @param message The message to write to the {@link BufferedChannel}.
     *
     * @return {@literal true} if the write succeeds, {@literal false} otherwise.
     *
     * @throws ClosedChannelException If the channel has already been closed for writing.
     */
    @Override
    public boolean tryWrite(T message) {
        Preconditions.checkNotNull(message);
        if (!open) {
            throw new ClosedChannelException("Channel is already closed for writing");
        }
        boolean success = internalQueue.offer(message);
        if (success) {
            runAfterWriteActions();
        }
        return success;
    }


    private void blockedWrite(T message, Optional<Integer> timeout, Optional<TimeUnit> unit)
        throws TimeoutException {
        Preconditions.checkNotNull(message);
        if (!open) {
            throw new ClosedChannelException("Channel is already closed for writing");
        }

        Thread currThread = Thread.currentThread();
        try {
            if (internalQueue.offer(message)) {
                runAfterWriteActions();
                return;
            }
            this.blockedWriters.put(currThread.getId(), currThread);
            if (timeout.isPresent()) {
                boolean success =
                    internalQueue.offer(message, timeout.get(), unit.orElse(MILLISECONDS));
                if (!success) {
                    throw new TimeoutException();
                }
            } else {
                internalQueue.put(message);
            }
            runAfterWriteActions();
        } catch (InterruptedException ex) {
            if (!open) {
                throw new ClosedChannelException("Channel got closed before write could complete");
            }
            throw new IllegalStateException();
        } finally {
            this.blockedWriters.remove(currThread.getId());
        }
    }

    /**
     * Returns {@literal true} if the channel can be written to.
     *
     * @return {@literal true} if the channel can be written to, else {@literal false}.
     */
    @Override
    public boolean canWrite() {
        return isOpen();
    }

    private void runAfterWriteActions() {
        for (AfterWriteAction afw : afterWriteActionList) {
            afw.onWrite();
        }
    }

    /**
     * Reads the next message from the channel. If the channel is currently empty, the thread blocks
     * until a message is available for reading.
     *
     * @throws NoSuchChannelElementException If there are no further element that can be available
     *                                       (because the {@link Channel} got closed).
     * @throws IllegalStateException         If there was an unexpected error in reading the
     *                                       channel.
     * @see Channel#read()
     */
    @Override
    public T read() throws NoSuchChannelElementException, IllegalStateException {
        return blockedRead(Optional.empty(), Optional.empty());
    }

    /**
     * Reads the next message from the channel. If the channel is currently empty, the read blocks
     * until a message is available or the timeout period is over.
     *
     * @param timeout The timeout value after which read times out.
     * @param unit    The unit corresponding to the timeout value.
     *
     * @return The next element from the {@link Channel}.
     *
     * @throws TimeoutException              If no message can be read within the given timeout
     *                                       period.
     * @throws NoSuchChannelElementException If there are no further element that can be available
     *                                       (because the {@link Channel} got closed).
     * @throws IllegalStateException         If there was an unexpected error in reading the
     *                                       channel.
     * @throws NullPointerException          If the read message was {@literal null}.
     */
    @Override
    public T read(int timeout, TimeUnit unit) throws TimeoutException {
        return blockedRead(Optional.of(timeout), Optional.ofNullable(unit));
    }

    /**
     * Tries to read the next message from the {@link Channel}. If the channel is empty, it returns
     * {@literal null}.
     *
     * @return The next message from the {@link Channel} or {@literal null} of the channel is empty.
     *
     * @throws NoSuchChannelElementException If there are no further element that can be read from
     *                                       the channel (because the channel has been closed).
     */
    @Override
    public T tryRead() {
        if (!open && internalQueue.isEmpty()) {
            throw new NoSuchChannelElementException();
        }
        return internalQueue.poll();
    }

    /**
     * Returns {@literal true} if the channel can be read.
     *
     * @return {@literal true} if the channel can be read, else {@literal false}.
     */
    @Override
    public boolean canRead() {
        return isOpen() || !internalQueue.isEmpty();
    }

    private T blockedRead(Optional<Integer> timeout, Optional<TimeUnit> unit)
        throws NoSuchChannelElementException, IllegalStateException {
        if (!open && internalQueue.isEmpty()) {
            throw new NoSuchChannelElementException();
        }
        Thread currThread = Thread.currentThread();


        try {
            T msg = internalQueue.poll();
            if (msg != null) {
                return msg;
            }
            this.blockedReaders.put(currThread.getId(), currThread);
            log.debug("Added thread {} as reader", currThread.getName());

            if (timeout.isPresent()) {
                msg = internalQueue.poll(timeout.get(), unit.orElse(MILLISECONDS));
                if (msg == null) {
                    throw new TimeoutException();
                }
            } else {
                msg = internalQueue.take();
            }
            return msg;
        } catch (InterruptedException ex) {
            if (!open && internalQueue.isEmpty()) {
                throw new NoSuchChannelElementException();
            }
            throw new IllegalStateException();
        } finally {
            this.blockedReaders.remove(Thread.currentThread().getId());
        }
    }

    /**
     * Closes this channel. On closure of this channel, all the threads that are stuck in {@link
     * #read()} or {@link #write(Object)} are interrupted. The iterator associated with this channel
     * is also closed along with the channel.
     *
     * @see Channel#close()
     */
    @Override
    public void close() {
        this.open = false;

        Set<Map.Entry<Long, Thread>> readers = this.blockedReaders.entrySet();
        synchronized (this.blockedReaders) {
            readers.forEach(ent -> {
                Thread thr = ent.getValue();
                log.debug("Read thread interrupted: {}", thr.getName());
                thr.interrupt();
            });
        }

        Set<Map.Entry<Long, Thread>> writers = this.blockedWriters.entrySet();
        synchronized (this.blockedWriters) {
            writers.forEach(ent -> {
                Thread thr = ent.getValue();
                log.debug("Write thread interrupted: {}", thr.getName());
                thr.interrupt();
            });
        }

        this.afterWriteActionList.forEach(afw -> {
            try {
                afw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        this.iterator.markDone();
    }

    /**
     * Checks if the channel has been closed.
     *
     * @return {@literal true}, if the channel has been closed, {@literal false} otherwise.
     */
    @Override
    public boolean isOpen() {
        return open;
    }

    /**
     * Returns a unique id for this {@link Channel}.
     *
     * @return A unique id for this {@link Channel}.
     */
    @Override
    public String getId() {
        return channelId;
    }

    /**
     * @see Channel#getDataType()
     */
    @Override
    public Class<T> getDataType() {
        return clazz;
    }

    @Override
    public void registerAfterWriteAction(AfterWriteAction afw) {
        if (this.afterWriteActionList.size() >= MAX_AFTER_WRITE_ACTIONS) {
            throw new TooManySelectorException(
                "Maximum number of AfterWriteActions registered on this channel");
        }
        this.afterWriteActionList.add(afw);
    }

    /**
     * The capacity of this {@link BufferedChannel}. Capacity is the number of messages that can be
     * inserted into the channel without a read before the writes are blocked.
     *
     * @return The capacity of this {@link BufferedChannel}
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Available slots in the channel. This is the difference between the capacity and the number of
     * messages already in the channel.
     *
     * @return The number of available slots in the channel.
     */
    public int getAvailable() {
        return capacity - internalQueue.size();
    }

    /**
     * Returns an {@link Iterator} to iterate over the channel. The iterator is a blocking iterator.
     * If no elements are present in the channel the iterators {@link Iterator#next()} gets blocked.
     * In case the channel is closed while iterating, a {@link NoSuchChannelElementException} is
     * thrown from the {@link Iterator#next()} method.
     *
     * @return An {@link Iterator} to iterate over the channel.
     *
     * @see Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    /**
     * Perform an action over the messages as received in the current thread from the channel. This
     * method will not propagate the {@link NoSuchChannelElementException} that is thrown when the
     * channel is closed. Returning from this method indicates the channel is closed, and the caller
     * is free to perform any cleanup tasks.
     *
     * @param action The action to perform for each message received on this thread.
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        try {
            while (this.isOpen() || !this.internalQueue.isEmpty()) {
                T msg = this.read();
                action.accept(msg);
            }
        } catch (ClosedChannelException | IllegalStateException ex) {
            // Done iterating. Do nothing
        } catch (NullPointerException ex) {
            // Should not happen
        }
    }
}
