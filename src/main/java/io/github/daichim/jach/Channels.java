package io.github.daichim.jach;

/**
 * Channels is a utility class that exposes common initialization and operations on channels.
 */
public class Channels {

    /**
     * Creates a new {@link Channel} with the given capacity of the backing store. The capacity
     * parameter determines the number of messages that can be in the {@link Channel} before the
     * next write blocks.
     *
     * @param capacity The capacity of the backing queue.
     * @param <T>      The type of the message in the {@link Channel}
     *
     * @return A new instance of {@link Channel} with the given capacity.
     */
//    public static <T> Channel<T> newChannel(int capacity) {
//        return new BufferedChannel<>(capacity);
//    }

    /**
     * Creates a new {@link Channel} which does not have any backing store. Every write will block
     * until the message written in the previous write has been successfully read from the channel
     * by another thread.
     *
     * @param <T> The type of the message in the {@link Channel}
     *
     * @return a new instance of {@link Channel} which does not have any backing store.
     */
//    public static <T> Channel<T> newUnbufferedChannel() {
//        return new UnbufferedChannel<>();
//    }

}
