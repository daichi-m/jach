package io.github.daichim.jach;

import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import io.github.daichim.jach.internal.ChannelIterator;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

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
    private final Map<Long, Thread> writeThreads;
    private final Map<Long, Thread> readThreads;
    private final ChannelIterator<T> iterator;
    private volatile boolean openState;


    public BufferedChannel(int capacity) {
        this.capacity = capacity;
        this.internalQueue = new ArrayBlockingQueue<>(capacity, true);
        this.openState = true;
        this.readThreads = Collections.synchronizedMap(new HashMap<>());
        this.writeThreads = Collections.synchronizedMap(new HashMap<>());
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
    public void write(T msg) throws ClosedChannelException, IllegalStateException {

        Preconditions.checkNotNull(msg);

        if (!openState) {
            throw new ClosedChannelException("Channel is already closed for writing");
        }
        boolean succ = internalQueue.offer(msg);
        if (succ) {
            return;
        }

        try {
            Thread currThread = Thread.currentThread();
            this.writeThreads.put(currThread.getId(), currThread);
            log.debug("Added thread {} as writer", currThread.getName());
            internalQueue.put(msg);
        } catch (InterruptedException ex) {
            if (!openState) {
                throw new ClosedChannelException("Channel got closed before write could complete");
            }
            throw new IllegalStateException();
        } finally {
            this.writeThreads.remove(Thread.currentThread().getId());
        }
    }

    /**
     * Reads the next message from the channel. If the channel is currently empty, the thread blocks
     * until a message is available for reading.
     *
     * @throws ClosedChannelException If the channel is already closed, or channel got closed before
     *                                the read could be completed.
     * @throws IllegalStateException  If there was an unexpected error in reading the channel.
     * @throws NullPointerException   If the read message was {@literal null}.
     * @see Channel#read()
     */
    @Override
    public T read() throws ClosedChannelException {
        if (!openState && internalQueue.size() == 0) {
            throw new ClosedChannelException("Channel has been closed for reading");
        }
        T msg = internalQueue.poll();
        if (msg != null) {
            return msg;
        }

        try {
            Thread currThread = Thread.currentThread();
            this.readThreads.put(currThread.getId(), currThread);
            log.debug("Added thread {} as reader", currThread.getName());
            msg = internalQueue.take();
            Preconditions.checkNotNull(msg);
            return msg;
        } catch (InterruptedException ex) {
            if (!openState && internalQueue.size() == 0) {
                throw new ClosedChannelException("Channel got closed before any read could happen");
            }
            throw new IllegalStateException();
        } finally {
            this.readThreads.remove(Thread.currentThread().getId());
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
        this.openState = false;

        Set<Map.Entry<Long, Thread>> readers = this.readThreads.entrySet();
        synchronized (this.readThreads) {
            readers.forEach(ent -> {
                Thread thr = ent.getValue();
                log.debug("Read thread interrupted: {}", thr.getName());
                thr.interrupt();
            });
        }

        Set<Map.Entry<Long, Thread>> writers = this.writeThreads.entrySet();
        synchronized (this.writeThreads) {
            writers.forEach(ent -> {
                Thread thr = ent.getValue();
                log.debug("Write thread interrupted: {}", thr.getName());
                thr.interrupt();
            });
        }
        this.iterator.markDone();
    }

    /**
     * Checks if the channel has been closed.
     *
     * @return {@literal true}, if the channel has been closed, {@literal false} otherwise.
     */
    @Override
    public boolean isClosed() {
        return !openState;
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
            while (!(this.isClosed() && this.internalQueue.isEmpty())) {
                T msg = this.read();
                action.accept(msg);
                if (Thread.currentThread().isInterrupted()) {

                }
            }
        } catch (ClosedChannelException | IllegalStateException ex) {
            // Done iterating. Do nothing
        } catch (NullPointerException ex) {
            // Should not happen
        }
    }
}
