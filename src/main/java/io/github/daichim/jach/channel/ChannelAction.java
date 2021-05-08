package io.github.daichim.jach.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Consumer;

/**
 * {@link ChannelAction} is a combination of a {@link Channel} and a {@link Consumer} that will be
 * executed on each message of the channel.
 */
@Getter
@AllArgsConstructor
public class ChannelAction<T> {
    Channel<T> channel;
    Consumer<T> action;

    /**
     * Create a {@link ChannelAction} for the given {@link Channel} with a given {@link Consumer}
     * action that is going to be executed for each message in the {@link Channel}.
     *
     * @param channel The {@link Channel} from which the message is read in.
     * @param action  The {@link Consumer} that will be executed for each message in the channel.
     * @param <T>     The type of the data that is stored in the {@link Channel} and will be passed
     *                on to the {@link Consumer}.
     *
     * @return The new {@link ChannelAction} object for this {@link Channel} and {@link Consumer}
     *     combination.
     */
    public static <T> ChannelAction<T> actOn(Channel<T> channel, Consumer<T> action) {
        return new ChannelAction<>(channel, action);
    }
}
