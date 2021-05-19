package io.github.daichim.samples;



import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;
import io.github.daichim.jach.time.Ticker;

import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachChannels.make;
import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;
import static io.github.daichim.jach.JachTime.ticker;
import static io.github.daichim.jach.channel.selector.Selector.BREAK_ACTION;
import static io.github.daichim.samples.Utilities.sleep;

public class Tickers {

    public static void main(String[] args) {

        Ticker ticker = ticker(500, TimeUnit.MILLISECONDS);
        Channel<Boolean> done = make(Boolean.class);

        Selector sel = selector(
            selectCase(done,
                BREAK_ACTION),
            selectCase(ticker.C,
                t -> System.out.println("Ticker ticked at " + t.toString()))
        );
        go(() -> {
            sel.untilDone();
            System.out.println("Selector complete " + !sel.isActive());
        });

        sleep(2000);
        ticker.stop();
        done.write(true);
        System.out.println("Ticker stopped");

    }
}
