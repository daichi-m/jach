package io.github.daichim.jach.time;

import io.github.daichim.jach.JachChannels;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.exception.ClosedChannelException;

import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Ticker {



    /**
     * The {@link Channel} of the {@link Ticker} on which an event is sent once every tick.
     */
    public Channel<Instant> C;

    private long duration;
    private TimeUnit unit;
    private volatile boolean open;
    private Future<?> tickerFuture;
    private ScheduledExecutorService executor;

    public Ticker(long duration, TimeUnit unit, ScheduledExecutorService executor) {
        this.duration = duration;
        this.unit = unit;
        this.C = JachChannels.make(Instant.class);
        this.open = true;
        this.executor = executor;

        this.tickerFuture =
            executor.scheduleAtFixedRate(() -> this.C.write(Instant.now()),
                duration, duration, unit);
    }

    /**
     * Stops the {@link Ticker} from sending further events. This method also cleans up existing
     * resources held by this {@link Ticker} instance. Any subsequent action on the {@link Ticker}
     * would throw {@link ClosedChannelException}.
     */
    public void stop() {
        this.open = false;
        this.tickerFuture.cancel(false);
        this.C.close();
    }

    /**
     * Resets the tick interval of this ticker to the new interval. Subsequent events on this ticker
     * will occur after the given interval.
     *
     * @param duration The new duration of the ticker.
     * @param unit     The unit corresponding to the duration value.
     *
     * @throws ClosedChannelException If the ticker has already been stopped.
     */
    public void reset(long duration, TimeUnit unit) throws ClosedChannelException {
        if (!open) {
            throw new ClosedChannelException("Timer has already been closed");
        }

        this.tickerFuture.cancel(false);
        this.duration = duration;
        this.unit = unit;
        this.tickerFuture = executor.scheduleAtFixedRate(() -> this.C.write(Instant.now()),
            duration, duration, unit);
    }

}