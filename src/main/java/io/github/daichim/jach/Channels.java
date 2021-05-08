package io.github.daichim.jach;

import io.github.daichim.jach.channel.ChannelAction;

import java.util.Arrays;

/**
 * Channels is a utility class that exposes common initialization and operations on channels.
 */
public class Channels {

    public static void select(ChannelAction... actions) {
        Arrays.stream(actions)
            .map(act -> act.getChannel())
            .forEach(chan -> {
                // Setup wait
            });


    }


}
