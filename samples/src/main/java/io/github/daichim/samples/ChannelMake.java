package io.github.daichim.samples;
import io.github.daichim.jach.channel.Channel;

import static io.github.daichim.jach.JachChannels.*;

public class ChannelMake {

    public static void main(String[] args) {
        Channel<String> messages = make(String.class);
        go(() -> messages.write("ping"));
        String msg = messages.read();
        System.out.println(msg);
    }
}
