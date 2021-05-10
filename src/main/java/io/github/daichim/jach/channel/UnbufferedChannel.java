package io.github.daichim.jach.channel;

import io.github.daichim.jach.channel.copier.Copier;
import io.github.daichim.jach.channel.copier.KryoCopier;

/**
 * {@link UnbufferedChannel} is a special {@link Channel} where only one message can be written and
 * subsequent writes block until that message is read by another thread. It can be treated like a
 * {@link BufferedChannel} with capacity of 1 message.
 *
 * @param <T> The type of the message which the {@link UnbufferedChannel} holds.
 */
public class UnbufferedChannel<T> extends BufferedChannel<T> {

    public UnbufferedChannel(Class<T> clazz) {
        super(1, clazz, new KryoCopier<>());
    }

    public UnbufferedChannel(Class<T> clazz, Copier<T> copier) {
        super(1, clazz, copier);
    }

}
