package io.github.daichim.jach.internal;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Writable} interface denotes a data-structure which can be used to write messages which can
 * then be read using a {@link Readable}. This interface is for internal purposes and should not be
 * used by client libraries.
 *
 * @param <T> The type of data that this interface can write.
 */
public interface Writable<T> extends AutoCloseable {

    /**
     * Writes the given message to the destination. If the destination does not have space, the
     * write blocks until space is freed up in the destination.
     *
     * @param message The message to write to the destination.
     *
     * @throws IllegalStateException If the destination has been closed by some other thread.
     */
    void write(T message) throws IllegalStateException;

    /**
     * Writes the given message to the destination. If the destination does not have  space, the
     * write blocks until the space is freed up in the destination or the call times out.
     *
     * @param message The message to write to the destination/
     * @param timeout The timeout value after which the write times out.
     * @param unit    The unit of the timeout value.
     *
     * @throws TimeoutException In case the write does not succeed after the given timeout.
     */
    void write(T message, int timeout, TimeUnit unit) throws TimeoutException;

    /**
     * Tries to write a message to the destination. If the destination does not have free space, the
     * call returns immediately with {@literal false}.
     *
     * @param message The message to write to the destination.
     *
     * @return {@literal true} if the write was successful, {@literal false} otherwise.
     */
    boolean tryWrite(T message);

    /**
     * The destination allows write to happen. It does not necessarily means the write will succeed,
     * it can block or timeout if there is no space, but if space is made available, the write will
     * eventually succeed.
     *
     * @return {@literal true} if the destination can be written to, {@literal false} otherwise.
     */
    boolean canWrite();


}
