package io.github.daichim.samples;

import io.github.daichim.jach.JachTime;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;

import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachChannels.makeStr;
import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;
import static io.github.daichim.jach.JachTime.after;
import static io.github.daichim.samples.Utilities.sleep;

public class Timeout {

    public static void main(String[] args) {
        doNotTimeout();
        timeout();
    }

    public static void doNotTimeout() {
        Channel<String> c1 = makeStr(1);
        go(() -> {
            sleep(1000);
            c1.write("result 1");
        });

        Selector sel = selector(
            selectCase(c1, System.out::println),
            selectCase(after(2, TimeUnit.SECONDS),
                instant -> System.out.println("Timed out in 1"))
        );
        sel.select();
    }

    public static void timeout() {
        Channel<String> c1 = makeStr(1);
        go(() -> {
            sleep(3000);
            c1.write("result 2");
        });

        Selector sel = selector(
            selectCase(c1, System.out::println),
            selectCase(after(2, TimeUnit.SECONDS),
                instant -> System.out.println("Timed out in 2"))
        );
        sel.select();
    }
}
