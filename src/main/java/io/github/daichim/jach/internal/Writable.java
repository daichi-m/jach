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
public interface Writable<T> {

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
     * Returns {@literal true} if the destination is not closed and has space to write messages. It
     * does not guarantee that the next write will succeed, since another thread might have written
     * a new message in between the time the caller checks this method and invokes the {@link
     * #write(Object)} call.
     *
     * @return If the destination is not closed and have space to write a message returns {@literal
     *     true}, otherwise returns {@literal false}.
     */
    boolean canWrite();


}
