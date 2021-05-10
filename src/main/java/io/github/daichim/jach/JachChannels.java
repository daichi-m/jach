package io.github.daichim.jach;

import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.channel.UnbufferedChannel;
import io.github.daichim.jach.channel.copier.KryoCopier;

/**
 * Channels is a utility class that exposes common initialization and operations on channels.
 */
public class JachChannels {

    public static BufferedChannel<String> makeStr(int capacity) {
        return make(String.class, capacity);
    }

    public static UnbufferedChannel<String> makeStr() {
        return make(String.class);
    }

    public static BufferedChannel<Integer> makeInt(int capacity) {
        return make(Integer.class, capacity);
    }

    public static UnbufferedChannel<Integer> makeInt() {
        return make(Integer.class);
    }

    public static <T> BufferedChannel<T> make(Class<T> type, int capacity) {
        return new BufferedChannel<>(capacity, type, new KryoCopier<>());
    }

    public static <T> UnbufferedChannel<T> make(Class<T> type) {
        return new UnbufferedChannel<>(type);
    }
}
