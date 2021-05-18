package io.github.daichim.jach;

import io.github.daichim.jach.channel.Action;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import io.github.daichim.jach.time.Ticker;
import io.github.daichim.jach.time.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.daichim.jach.JachChannels.go;

/**
 * {@link JachTime} contains utility methods which exposes methods related to time using {@link
 * Channel}.
 */
@Slf4j
public class JachTime {

    private static ScheduledExecutorService executor;

    static {
        executor = Executors.newScheduledThreadPool(10);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {
                executor.shutdown();
                executor.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }

        }));
    }

    /**
     * Returns a {@link Channel} which will send the current time after the given duration expires.
     *
     * @param duration The duration after which to send the event.
     * @param unit     The unit corresponding to the duration.
     *
     * @return A {@link Channel} on which the event will be sent to.
     */
    public static Channel<Instant> after(long duration, TimeUnit unit) {
        return timer(duration, unit).C;
    }

    /**
     * Returns a new {@link Ticker} instance which will send the current time on it's {@link
     * Channel} at every duration.
     *
     * @param duration The duration between each tick;
     * @param unit     The time unit corresponding to the duration.
     *
     * @return A {@link Ticker} instance that sends a message (current time) on every tick interval.
     */
    public static Ticker ticker(long duration, TimeUnit unit) {
        return new Ticker(duration, unit, executor);
    }

    /**
     * Returns a new {@link Timer} instance which sends the current time on it's {@link Channel}
     * once the timer expires.
     *
     * @param duration The duration after which the {@link Timer} will fire.
     * @param unit     The time unit corresponding to the duration.
     *
     * @return A {@link Timer} that will fire after the given duration.
     */
    public static Timer timer(long duration, TimeUnit unit) {
        return new Timer(duration, unit, executor);
    }

    /**
     * Execute an action after the given duration in a different thread.
     *
     * @param duration The duration of time
     * @param unit     The unit that is associated with the duration
     * @param action   The action to execute after the duration expires.
     *
     * @return A {@link Timer} instance which can be used to cancel the action using {@link
     *     Timer#close()}.
     */
    public static Timer afterFunc(long duration, TimeUnit unit, Action action) {
        Timer timer = timer(duration, unit);
        go(() -> {
            try {
                timer.C.read();
            } catch (NoSuchChannelElementException ex) {
                log.debug("Timer cancelled, bailing out");
                return;
            }
            action.accept();
        });
        return timer;
    }

    public static void setExecutor(ScheduledExecutorService executor) {
        JachTime.executor.shutdown();
        JachTime.executor = executor;
    }

}
