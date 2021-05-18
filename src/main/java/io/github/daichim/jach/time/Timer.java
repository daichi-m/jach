package io.github.daichim.jach.time;

import io.github.daichim.jach.JachChannels;
import io.github.daichim.jach.channel.Action;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.ClosedChannelException;

import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link Timer} represents a single event. It is always instantiated with a fixed duration,
 * after which it sends an event to it's channel and can optionally execute an {@link Action}.
 */
public class Timer {

    /**
     * The {@link Channel} of the {@link Timer} on which an event is sent once the timer
     * expires.
     */
    public Channel<Instant> C;

    private long duration;
    private TimeUnit unit;
    private volatile boolean open;
    private Future<?> timerFuture;
    private ScheduledExecutorService executor;

    public Timer(long duration, TimeUnit unit, ScheduledExecutorService executor) {
        this.duration = duration;
        this.unit = unit;
        this.C = JachChannels.make(Instant.class);
        this.open = true;
        this.executor = executor;

        this.timerFuture =
            executor.schedule(() -> {
                this.C.write(Instant.now());
                this.close();
            }, duration, unit);
    }

    /**
     * Closes the timer. Anu subsequent operation on the timer would throw a {@link
     * ClosedChannelException}.
     */
    public void close() {
        this.open = false;
        this.timerFuture.cancel(false);
        this.C.close();
    }

    /**
     * Resets the duration of the timer to the new duration.
     *
     * @param duration The new duration for the {@link Timer}
     * @param unit     The unit corresponding to the duration.
     *
     * @throws ClosedChannelException If the {@link Timer} has already been closed or expired.
     */
    public void reset(long duration, TimeUnit unit) throws ClosedChannelException {
        if (!open) {
            throw new ClosedChannelException("Timer has already been closed");
        }

        this.timerFuture.cancel(false);
        this.duration = duration;
        this.unit = unit;
        this.timerFuture = executor.schedule(() -> this.C.write(Instant.now()), duration, unit);
    }
}
