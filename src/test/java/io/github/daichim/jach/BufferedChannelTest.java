package io.github.daichim.jach;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.daichim.jach.exception.ClosedChannelException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class BufferedChannelTest {

    public static final int CAPACITY = 5;
    public static final int LIFE_UNIVERSE_AND_EVERYTHING = 42;
    private static final int SLEEP_INTERVAL = 500;

    private ExecutorService threadPool;

    private BufferedChannel<Integer> testChannel;

    @BeforeClass
    public void setupClass() {
        threadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("test-thread-%d").build());
    }

    @BeforeMethod
    public void initializeChannelForWrite() {
        this.testChannel = new BufferedChannel<>(CAPACITY);
        log.info("Channel initialized");
    }

    @Test(groups = "channel_write", description = "Successfully write messages to channel")
    public void writeTestSuccess() throws Exception {
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {

            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY; i++) {
                    testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
                    log.debug("Message id {} written", i);
                }
            });
            fut.get(1, TimeUnit.SECONDS);
            Assert.assertTrue(fut.isDone());
            log.info("Messages written without blocking");
        }
    }

    @Test(expectedExceptions = TimeoutException.class, groups = "channel_write",
        description = "Blocks writing to channel due to buffer full")
    public void writeTestBlocks() throws Exception {
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
                    log.debug("Message id {} written", i);
                }
            });
            fut.get(2, TimeUnit.SECONDS);
            Assert.assertFalse(fut.isDone());
            log.info("Thread is blocked in write");
        }
    }

    @Test(groups = "channel_write",
        description = "Blocks writing to channel due to buffer full, "
            + "but then unblocked due to a subsequent read")
    public void writeTestBlocksAndUnblocks() throws Exception {
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
                    log.debug("Message id {} written", i);
                }
            });
            try {
                fut.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException ignored) {
            }
            Assert.assertFalse(fut.isDone());
            log.info("Thread is blocked in write");

            int msg = testChannel.read();
            log.debug("Read in message {}", msg);
            fut.get(1, TimeUnit.SECONDS);
            Assert.assertTrue(fut.isDone());
            log.info("Thread got unblocked after one write");
        }
    }

    @Test(expectedExceptions = ClosedChannelException.class, groups = "channel_write",
        description = "Write to a closed channel")
    public void writeToClosedChannel() throws Exception {
        Future<?> fut = null;
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
                    log.debug("Message id {} written", i);
                }
            });
            TimeUnit.MILLISECONDS.sleep(SLEEP_INTERVAL);
        }
        try {
            fut.get();
        } catch (ExecutionException ex) {
            log.warn("Exception raised in thread: {}", ex.getCause().getClass());
            throw (Exception) ex.getCause();
        }
    }

    @Test(expectedExceptions = ClosedChannelException.class, groups = "channel_write",
        description = "Write to a closed channel - 2")
    public void writeToClosedChannel2() throws Exception {
        testChannel.close();
        testChannel.write(42);
    }

    @Test(groups = "channel_read", description = "Reads messages successfully from the channel")
    public void readTestSuccess() throws Exception {

        // Pre-populate the channel
        for (int i = 0; i < CAPACITY; i++) {
            testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
        }

        Future<?> fut = threadPool.submit(() -> {
            for (int i = 0; i < CAPACITY; i++) {
                int msg = testChannel.read();
                log.debug("Message read from channel: {}", msg);
                Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
            }
        });
        fut.get(1, TimeUnit.SECONDS);
        Assert.assertTrue(fut.isDone());
        log.info("Succesfully read in {} messages without blocking", CAPACITY);
    }

    @Test(groups = "channel_read", description = "Blocks while reading a channel because there is"
        + " no data")
    public void readTestBlocks() throws Exception {

        Future<?> fut = threadPool.submit(() -> {
            int msg = testChannel.read();
            log.debug("Message read in {}", msg);
            Assert.assertTrue(msg >= 0 && msg < 100);
        });
        try {
            fut.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException ignored) {
        }
        Assert.assertFalse(fut.isDone());
        log.info("Read is blocked because of no data in channel");
        fut.cancel(true);
    }

    @Test(groups = "channel_read", description = "Blocks while reading a channel because of no "
        + "data, then unblocks due to a write")
    public void readTestBlocksThenUnblocks() throws Exception {
        Future<?> fut = threadPool.submit(() -> {
            int msg = testChannel.read();
            log.debug("Message read in {}", msg);
            Assert.assertTrue(msg >= 0 && msg < 100);
        });
        try {
            fut.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException ignored) {
        }
        Assert.assertFalse(fut.isDone());
        log.info("Read is blocked because of no data in channel");

        testChannel.write(42);
        fut.get(1, TimeUnit.SECONDS);
        Assert.assertTrue(fut.isDone());
        log.info("Read is unblocked now after data is inserted");
    }

    @Test(groups = "channel_read", description = "Read from a close channel",
        expectedExceptions = ClosedChannelException.class)
    public void readTestClosedChannel() throws Exception {
        testChannel = new BufferedChannel<>(CAPACITY);
        Future<?> fut = threadPool.submit(() -> {
            int msg = testChannel.read();
            Assert.fail("Read in without blocking");
        });
        testChannel.close();
        try {
            fut.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw (Exception) ex.getCause();
        }
    }

    @Test(groups = "channel_read", description = "Read from a close channel",
        expectedExceptions = ClosedChannelException.class)
    public void readTestClosedChannel2() throws Exception {
        testChannel = new BufferedChannel<>(CAPACITY);
        testChannel.close();
        testChannel.read();
    }

    @Test(groups = "channel_close", description = "Close channel - check readers are interrupted")
    public void closeChannelReadInterruptTest() throws Exception {

        Map<Integer, Future<?>> futs = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Future<?> fut = threadPool.submit(() -> {
                testChannel.read();
            });
            futs.put(i, fut);
        }
        TimeUnit.MILLISECONDS.sleep(1);
        testChannel.close();

        for (Map.Entry<Integer, Future<?>> f : futs.entrySet()) {
            try {
                f.getValue().get(1, TimeUnit.SECONDS);
                Assert.fail(String.format("Thread %d was not interrupted ", f.getKey()));
            } catch (ExecutionException ex) {
                Assert.assertTrue(ex.getCause() instanceof ClosedChannelException);
            }
        }
        log.info("All 10 read threads were interrupted");
    }

    @Test(groups = "channel_close", description = "Close channel - check writers are interrupted")
    public void closeChannelWriteInterruptTest() throws Exception {

        for (int i = 0; i < CAPACITY; i++) {
            testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
        }

        Map<Integer, Future<?>> futs = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            Future<?> fut = threadPool.submit(() -> {
                testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
            });
            futs.put(i, fut);
        }
        TimeUnit.MILLISECONDS.sleep(1);
        testChannel.close();

        for (Map.Entry<Integer, Future<?>> f : futs.entrySet()) {
            try {
                f.getValue().get(1, TimeUnit.SECONDS);
                Assert.fail(String.format("Thread %d was not interrupted ", f.getKey()));
            } catch (ExecutionException ex) {
                Assert.assertTrue(ex.getCause() instanceof ClosedChannelException);
            }
        }
        log.info("All 10 write threads were interrupted");
    }

    @Test(groups = "channel_close", description = "Check channel is closed")
    public void isChannelClosedTest() throws Exception {
        testChannel.close();
        Assert.assertTrue(testChannel.isClosed());
    }

    @Test(groups = "channel_cap", description = "Check channel capacity")
    public void channelCapacityTest() throws Exception {
        int cap = testChannel.getCapacity();
        Assert.assertEquals(cap, CAPACITY);
    }

    @Test(groups = "channel_cap", description = "Check channel availability")
    public void channelAvailabilityTest() throws Exception {
        int avl = testChannel.getAvailable();
        Assert.assertEquals(avl, CAPACITY);
        Future<?> fut = threadPool.submit(() -> {
            while(true) {
                testChannel.write(LIFE_UNIVERSE_AND_EVERYTHING);
                TimeUnit.MILLISECONDS.sleep(100);
            }
        });
        avl = testChannel.getAvailable();
        Assert.assertTrue(avl >= 0 && avl <= CAPACITY);
        TimeUnit.MILLISECONDS.sleep(200);
        avl = testChannel.getAvailable();
        Assert.assertTrue(avl >= 0 && avl <= CAPACITY);
        fut.cancel(true);
    }


    @Test(groups = "channel_iterate", description = "Iterate over a channel and perform action")
    public void forEachTest() throws Exception {
        AtomicInteger increasingCtr = new AtomicInteger(1);
        AtomicInteger actionCtr = new AtomicInteger(0);
        Consumer<Integer> action = msg -> {
            log.debug("Read in {}", msg);
            Assert.assertTrue(msg < increasingCtr.get());
            actionCtr.incrementAndGet();
        };
        Future<?> fut = threadPool.submit(() -> {
            for (int i=0; i<100; i++) {
                testChannel.write(increasingCtr.getAndIncrement());
                log.debug("Written value: {}", increasingCtr.get());
            }
            testChannel.close();
        });
        TimeUnit.MILLISECONDS.sleep(10);
        testChannel.forEach(action);
        Assert.assertEquals(actionCtr.get(), 100);
    }

    @AfterMethod
    public void closeChannel() throws Exception {
        if (testChannel != null) {
            testChannel.close();
            testChannel = null;
        }
        log.info("Channel closed and nullified");
    }


    @AfterClass
    public void cleanupClass() {
        threadPool.shutdownNow();
    }
}
