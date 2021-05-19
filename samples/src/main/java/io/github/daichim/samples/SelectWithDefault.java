package io.github.daichim.samples;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;

import static io.github.daichim.jach.JachChannels.make;
import static io.github.daichim.jach.JachChannels.makeStr;
import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;

public class SelectWithDefault {

    public static void main(String[] args) {

        Channel<String> messages = makeStr();
        Channel<Boolean> signals = make(Boolean.class);

        Selector sel = selector(
            selectCase(messages,
                msg -> System.out.println("Received messages: " + msg)),
            selectCase(signals,
                sig -> System.out.println("Signal received: " + sig))
        );

        sel.untilOrDefault(() -> System.out.println("No activity"));

    }

}
