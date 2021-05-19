package io.github.daichim.samples;

import io.github.daichim.jach.JachTime;
import io.github.daichim.jach.time.Timer;

import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachTime.timer;
import static io.github.daichim.samples.Utilities.sleep;

public class Timers {

    public static void main(String[] args) {

        Timer timer1 = timer(2, TimeUnit.SECONDS);
        timer1.C.read();
        System.out.println("Timer1 fired");

        Timer timer2 = timer(1, TimeUnit.SECONDS);
        go(() -> {
            timer2.C.read();
            System.out.println("Timer2 fired");
        });
        timer2.close();
        sleep(2000);
    }


}
