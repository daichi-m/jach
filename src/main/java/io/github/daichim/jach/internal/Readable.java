package io.github.daichim.jach.internal;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Readable} interface denotes a data-structure which can be used to read messages from some
 * source. This interface is for internal purposes and should not be used by client libraries.
 *
 * @param <T> The type of data that this interface can read.
 */

public interface Readable<T> extends AutoCloseable {

    /**
     * Reads the next message from the source. If there are no elements in the source, the read
     * blocks until an element is available. If the source declared no more element will be
     * available, {@link NoSuchElementException} will be thrown.
     *
     * @return The next message from the source if available.
     *
     * @throws NoSuchElementException If the source declared that no more elements will be
     *                                available.
     */
    T read() throws NoSuchElementException;

    /**
     * Reads a message from the source. If there is no message, the read blocks until it times out
     * after the provided timeout value.
     *
     * @param timeout The timeout value after which read times out.
     * @param unit    The unit corresponding to the timeout value.
     *
     * @return The next message from the source if available within the timeout.
     *
     * @throws TimeoutException If no message was available within the timeout window to read from
     *                          the source.
     */
    T read(int timeout, TimeUnit unit) throws TimeoutException;

    /**
     * Tries to read a message from the source. If there is no message, the method will return
     * {@literal null}.
     *
     * @return The next message from the source if available, {@literal null} otherwise.
     */
    T tryRead();

    /**
     * The source can be read from. It does not necessarily means the read will succeed as it can
     * get blocked or timeout, but it means that the source still allows reads to happen.
     *
     * @return {@literal true} if the source can be read from, {@literal false} otherwise.
     */
    boolean canRead();

}
