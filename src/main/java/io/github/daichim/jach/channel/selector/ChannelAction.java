package io.github.daichim.jach.channel.selector;

import io.github.daichim.jach.channel.Channel;
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
}
