package io.github.daichim.jachsamples;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.routines.Routines;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachChannels.make;

public class ChannelSync {

    public static void main(String[] args) {

        Routines.Routine1<Channel<Boolean>> workerRoutine = ChannelSync::worker;
        Channel<Boolean> done = make(Boolean.class);
        go(workerRoutine, done);
        done.read();
        System.out.println("Worker completed");
    }

    @SneakyThrows
    private static void worker(Channel<Boolean> chan) {
        System.out.print("Working...");
        TimeUnit.SECONDS.sleep(1);
        System.out.println("Done");
        chan.write(true);
    }
}
