package io.github.daichim.jach.channel.copier;

import io.github.daichim.jach.exception.CopyException;

/**
 * {@link RefCopier} is the simplest {@link Copier} that returns the same object reference as the
 * parameter. {@link RefCopier} is an identity function.
 */
public class RefCopier<T> implements Copier<T> {

    /**
     * @see Copier#copyOf(Object)
     */
    @Override
    public T copyOf(T message) throws CopyException {
        return message;
    }
}
