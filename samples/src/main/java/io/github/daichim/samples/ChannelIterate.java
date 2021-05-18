package io.github.daichim.samples;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.NoSuchChannelElementException;

import static io.github.daichim.jach.JachChannels.makeStr;

public class ChannelIterate {

    public static void main(String[] args) {
        Channel<String> queue = makeStr(2);
        queue.write("one");
        queue.write("two");


        try {
            for (String elem : queue) {
                System.out.println(elem);
            }
        } catch (NoSuchChannelElementException ignored) { }
    }

}
