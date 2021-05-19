package io.github.daichim.jach.time;

import io.github.daichim.jach.JachTime;
import io.github.daichim.jach.channel.Action;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TimerTest {

    @Test
    public void normalTimerTest() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger interval = new AtomicInteger(1000);
        Timer timer = new Timer(interval.get(), TimeUnit.MILLISECONDS, executor);
        Instant start = Instant.now();
        Instant tick = timer.C.read();
        Duration duration = Duration.between(start, tick);
        Assert.assertTrue(duration.toMillis() >= interval.get() - 10);
    }

    @Test(expectedExceptions = NoSuchChannelElementException.class)
    public void timerStopTest() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Timer timer = new Timer(1000, TimeUnit.MILLISECONDS, executor);
        executor.schedule(timer::close, 500, TimeUnit.MILLISECONDS);
        Instant tick = timer.C.read();
    }

    @Test
    public void resetTimerTest() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        int interval = 1000;
        int resetInterval = 750;
        Timer timer = new Timer(interval, TimeUnit.MILLISECONDS, executor);
        Instant start = Instant.now();
        executor.schedule(() -> timer.reset(resetInterval, TimeUnit.MILLISECONDS),
            250, TimeUnit.MILLISECONDS);
        Instant tick = timer.C.read();
        Duration duration = Duration.between(start, tick);
        Assert.assertTrue(duration.toMillis() >= interval - 10);
    }

    @Test(timeOut = 2000)
    public void afterFuncTest() throws Exception {
        int interval = 1000;
        Instant start = Instant.now();
        CountDownLatch latch = new CountDownLatch(1);
        Action action = () -> {
            try {
                Duration duration = Duration.between(start, Instant.now());
                long millis = duration.toMillis();
                log.debug("Duration between ticks: {}", millis);
                Assert.assertTrue(millis >= interval);
            } finally {
                latch.countDown();
            }

        };
        JachTime.afterFunc(interval, TimeUnit.MILLISECONDS, action);
        latch.await();
    }

    @Test(timeOut = 2000)
    public void afterFuncCancelledTest() throws Exception {
        int interval = 1000;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Action action = Assert::fail;
        Timer timer = JachTime.afterFunc(interval, TimeUnit.MILLISECONDS, action);
        Future<?> fut = executor.schedule(timer::close, 500, TimeUnit.MILLISECONDS);
        fut.get();
    }

}