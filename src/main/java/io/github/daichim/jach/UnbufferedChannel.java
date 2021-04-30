package io.github.daichim.jach;

/**
 * {@link UnbufferedChannel} is a special {@link Channel} where only one message can be written and
 * subsequent writes block until that message is read by another thread. It can be treated like a
 * {@link BufferedChannel} with capacity of 1 message.
 *
 * @param <T> The type of the message which the {@link UnbufferedChannel} holds.
 */
public class UnbufferedChannel<T> extends BufferedChannel<T> {

    public UnbufferedChannel() {
        super(1);
    }



}
