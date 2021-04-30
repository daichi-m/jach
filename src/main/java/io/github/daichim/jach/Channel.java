package io.github.daichim.jach;

import io.github.daichim.jach.exception.ClosedChannelException;

import java.util.Spliterator;

/**
 * {@link Channel} is equivalent of a Golang channel in Java. It behaves like a queue on which
 * messages can be passed from one thread to another. The channel can be either buffered or
 * unbuffered.
 * <p>
 * Channel implement the {@link Iterable} interface, so they can be used in a for-each loop. But, it
 * does not allow splitting, hence parallel streams are not supported on a Channel.
 * <p>
 * Channel is also {@link AutoCloseable} so it can be initialized in a try-with-resource block.
 *
 * @param <T> The type of the message which the {@link Channel} holds.
 */
public interface Channel<T> extends AutoCloseable, Iterable<T> {

    /**
     * Write the data into the channel.
     *
     * @param data The message that has to be written to the channel.
     *
     * @throws ClosedChannelException If there is an attempt to write to a closed channel.
     */
    void write(T data) throws ClosedChannelException;

    /**
     * Read the next item from the channel. If the channel is empty this will block.
     *
     * @return The next item from the channel.
     *
     * @throws ClosedChannelException If there is an attempt to read from a closed channel.
     */
    T read() throws ClosedChannelException;

    /**
     * Closes the channel. Read and write operation on a closed channel would throw a {@link
     * ClosedChannelException}
     */
    void close();

    /**
     * Returns {@literal true} in case the channel is closed, else return {@literal false}
     *
     * @return {@literal true} if the channel has been closed, {@literal false} otherwise.
     */
    boolean isClosed();

    /**
     * {@link Spliterator} is not supported for a {@link Channel}. It will throw a {@link
     * UnsupportedOperationException}.
     *
     * @see Channel#spliterator()
     */
    @Override
    default Spliterator<T> spliterator() {
        throw new UnsupportedOperationException("Channel cannot be split");
    }

}
