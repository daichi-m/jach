package io.github.daichim.samples;

import io.github.daichim.jach.channel.Channel;

import static io.github.daichim.jach.JachChannels.make;

public class ChannelBuffering {

    public static void main(String[] args) {
        Channel<String> messages = make(String.class, 2);
        messages.write("buffered");
        messages.write("channel");

        System.out.println(messages.read());
        System.out.println(messages.read());
    }

}
