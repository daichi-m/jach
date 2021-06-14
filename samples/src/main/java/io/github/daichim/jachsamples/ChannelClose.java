package io.github.daichim.jachsamples;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.NoSuchChannelElementException;

import static io.github.daichim.jach.JachChannels.go;
import static io.github.daichim.jach.JachChannels.make;

public class ChannelClose {

    public static void main(String[] args) {

        Channel<Integer> jobs = make(Integer.class, 5);
        Channel<Boolean> done = make(Boolean.class);

        go( () -> {
            while (true) {
                try {
                    Integer j = jobs.read();
                    System.out.println("Job received " + j);
                } catch (NoSuchChannelElementException ex) {
                    System.out.println("All jobs received");
                    done.write(true);
                    return;
                }
            }
        });

        for (int j=0; j<5; j++) {
            jobs.write(j);
            System.out.println("Send job " + j);
        }
        jobs.close();
        System.out.println("Sent all jobs and closed jobs channel");
        done.read();
    }
}
