package io.github.daichim.jach.time;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.IntSummaryStatistics;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.daichim.jach.JachChannels.makeStr;
import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;

@Slf4j
public class TickerTest {

    Ticker ticker;

    @Test
    public void normalTickerTest() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final int tickInterval = 100;
        Ticker ticker = new Ticker(tickInterval, TimeUnit.MILLISECONDS, executor);
        TickCounter ctr = new TickCounter();
        Channel<String> exitCh = makeStr();
        IntSummaryStatistics stats = new IntSummaryStatistics();
        Selector selector = selector(
            selectCase(ticker.C, instant -> {
                long duration = ctr.tick();
                stats.accept((int) duration);
                log.debug("Duration: {} millis", duration);
                Assert.assertTrue(duration >= tickInterval - 10);
                Assert.assertTrue(duration <= tickInterval + 10);
            }),
            selectCase(exitCh, Selector.BREAK_ACTION));
        executor.schedule(() -> exitCh.write("EXIT"), 2, TimeUnit.SECONDS);
        selector.untilDone();

        log.debug("Statistics: {}", stats);
        Assert.assertEquals((int) Math.round(stats.getAverage()), tickInterval);
        Assert.assertEquals(ctr.count.get(), 2000 / tickInterval);
    }

    @Test(timeOut = 3000)
    public void stopTickerTest() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final int tickInterval = 100;
        AtomicBoolean stopped = new AtomicBoolean(false);
        Ticker ticker = new Ticker(tickInterval, TimeUnit.MILLISECONDS, executor);
        TickCounter ctr = new TickCounter();
        Selector selector = selector(
            selectCase(ticker.C, instant -> {
                Assert.assertFalse(stopped.get());
                long duration = ctr.tick();
                log.debug("Duration: {} millis", duration);
                Assert.assertTrue(duration >= tickInterval - 10);
                Assert.assertTrue(duration <= tickInterval + 10);

            }));
        executor.schedule(() -> {
            ticker.stop();
            stopped.set(true);
        }, 2, TimeUnit.SECONDS);
        selector.untilDone();
        Assert.assertTrue(stopped.get());
        Assert.assertEquals(ctr.count.get(), 2000 / tickInterval);
    }

    @Test(timeOut = 3000)
    public void resetTickerTest() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        AtomicInteger tickInterval = new AtomicInteger(100);
        AtomicBoolean stopped = new AtomicBoolean(false);
        Ticker ticker = new Ticker(tickInterval.get(), TimeUnit.MILLISECONDS, executor);
        TickCounter ctr = new TickCounter();
        Selector selector = selector(
            selectCase(ticker.C, instant -> {
                long duration = ctr.tick();
                log.debug("Duration: {} millis", duration);
                Assert.assertTrue(duration >= tickInterval.get() - 10);
                Assert.assertTrue(duration <= tickInterval.get() + 10);

            }));
        executor.schedule(() -> {
            ticker.reset(50, TimeUnit.MILLISECONDS);
            tickInterval.compareAndSet(100, 50);
        }, 1, TimeUnit.SECONDS);
        executor.schedule(() -> {
            ticker.stop();
            stopped.set(true);
        }, 2, TimeUnit.SECONDS);
        selector.untilDone();
        Assert.assertTrue(stopped.get());
    }

    private class TickCounter {
        Instant prevTick;
        AtomicInteger count;

        public TickCounter() {
            this.prevTick = Instant.now();
            this.count = new AtomicInteger(0);
        }

        public long tick() {
            Duration dur = Duration.between(prevTick, Instant.now());
            this.prevTick = Instant.now();
            count.incrementAndGet();
            return dur.toMillis();
        }
    }
}