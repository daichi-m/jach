package io.github.daichim.jach.channel.copier;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.CopyException;

/**
 * {@link Copier} provides the mechanism by which a {@link Channel} copies the incoming message to
 * it's in-memory data-structure.
 *
 * @param <T> The type of data that has to be copied.
 */
@FunctionalInterface
public interface Copier<T> {

    /**
     * Returns a copy of the message that should be stored in the in-memory data structure of the
     * {@link Channel}.
     *
     * @param message The message to store in the {@link Channel}.
     *
     * @return Copy of the message to store in-memory of the {@link Channel}.
     *
     * @throws CopyException In case the copy fails.
     */
    T copyOf(T message) throws CopyException;

}
