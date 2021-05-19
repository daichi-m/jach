package io.github.daichim.samples;

import io.github.daichim.jach.JachChannels;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachChannels.makeStr;
import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;
import static io.github.daichim.samples.Utilities.sleep;

public class Select {

    public static void main(String[] args) {

        Channel<String> c1 = makeStr();
        Channel<String> c2  = makeStr();

        go(() -> {
            sleep(1000);
            c1.write("one");
        });
        go(() -> {
            sleep(2000);
            c2.write("two");
        });

        Selector selector = selector(
          selectCase(c1, msg -> System.out.println("Received " + msg)),
          selectCase(c2, msg -> System.out.println("Received " + msg))
        );
        for (int i=0; i<2; i++) {
            selector.select();
        }
    }

}
