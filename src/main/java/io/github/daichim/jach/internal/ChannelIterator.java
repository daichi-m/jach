package io.github.daichim.jach.internal;

import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * An {@link Iterator} for {@link BufferedChannel}. It is a blocking iterator, where if there is no
 * message on the channel, the iterator will block until a new message is read in that thread or the
 * channel is closed.
 */
public class ChannelIterator<T> implements Iterator<T> {

    private final BufferedChannel<T> parentChannel;
    private volatile boolean done;

    public ChannelIterator(BufferedChannel<T> parentChannel) {
        this.parentChannel = parentChannel;
        this.done = false;
    }

    /**
     * Returns {@literal true} if the parent channel has not been closed, {@literal false}
     * otherwise.
     *
     * @return {@literal true} if the parent channel has not been closed, {@literal false}
     *     otherwise.
     */
    @Override
    synchronized public boolean hasNext() {
        return !this.done;
    }

    /**
     * Gets the next message for this thread from the underlying channel. If no message is
     * available, this calls blocks until a new message is read in this thread or the channel is
     * closed.
     *
     * @return The next message that is read from the channel.
     *
     * @throws NoSuchChannelElementException If the channel is closed before the next message could
     *                                       be read successfully.
     */
    @Override
    public T next() throws NoSuchChannelElementException {
        if (!hasNext()) {
            throw new NoSuchChannelElementException();
        }
        try {
            return parentChannel.read();
        } catch (ClosedChannelException | IllegalStateException ex) {
            this.done = true;
            throw new NoSuchChannelElementException();
        } catch (NullPointerException ex) {
            return next();
        }
    }

    /**
     * Remove is unsupported operation for a channel. This always throws an {@link
     * UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException Always.
     */
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot remove from channel");
    }

    /**
     * Runs the give action for each of the remaining messages that are read in this thread from the
     * underlying channel, until the channel is closed. When the channel is closed, this method
     * returns without throwing the {@link NoSuchChannelElementException} to the caller.
     */
    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        try {
            while (this.hasNext()) {
                T val = this.next();
                action.accept(val);
            }
        } catch (NoSuchElementException ex) {
        }
    }

    /**
     * Mark the iterator as done (i.e., the underlying channel has been closed).
     */
    synchronized public void markDone() {
        this.done = true;
    }
}
