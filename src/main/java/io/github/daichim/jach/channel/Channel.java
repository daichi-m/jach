package io.github.daichim.jach.channel;

import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.internal.AfterWriteAction;
import io.github.daichim.jach.internal.Readable;
import io.github.daichim.jach.internal.Writable;

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
 * <p>
 * <p>
 * There are certain minimal restrictions on the type of data the {@link Channel} can hold. The
 * following points need to be satisfied in order for the type to be compatible with {@link
 * Channel}:
 * <ol>
 *     <li>The type must have a nullary constructor.</li>
 *     <li>{@link Channel} internally uses the empty object as a "poison" object. Inserting an
 *     empty object of the type might cause the channel to close unexpectedly.</li>
 * </ol>
 *
 * @param <T> The type of the message which the {@link Channel} holds.
 */
public interface Channel<T> extends Writable<T>, Readable<T>, Iterable<T> {

    /**
     * Closes the channel. Read and write operation on a closed channel would throw a {@link
     * ClosedChannelException}
     */
    void close();

    /**
     * Returns {@literal true} in case the channel is open, else return {@literal false}
     *
     * @return {@literal true} if the channel has been open, {@literal false} otherwise.
     */
    boolean isOpen();

    /**
     * Returns a unique identifier for the {@link Channel}.
     *
     * @return A unique identifier for this {@link Channel}
     */
    String getId();

    /**
     * Returns the type of the element that is stored in this {@link Channel}
     *
     * @return The concrete class of type T that is stored in this {@link Channel}.
     */
    Class<T> getDataType();


    /**
     * ** For internal uses only **. Register an {@link AfterWriteAction} to this {@link Channel}.
     */
    void registerAfterWriteAction(AfterWriteAction afw);

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
