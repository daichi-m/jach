package io.github.daichim.jach.selector;

import io.github.daichim.jach.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

@Getter
@Setter
@AllArgsConstructor
public class ChannelAction<T> {
    Channel<T> channel;
    Consumer<T> action;
    Class<T> clazz;

    public static <T> ChannelAction<T> actOn(Channel<T> channel, Consumer<T> action,
                                             Class<T> clazz) {
        return new ChannelAction<>(channel, action, clazz);
    }
}
