package io.github.daichim.jach;

import io.github.daichim.jach.exception.ClosedChannelException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BufferedChannelTest {

    public static final int CAPACITY = 5;

    private ExecutorService threadPool;

    private BufferedChannel<Integer> testChannel;

    @BeforeClass
    public void setupClass() {
        threadPool = Executors.newCachedThreadPool();
    }

    @BeforeMethod
    public void initializeChannelForWrite() {
        this.testChannel = new BufferedChannel<>(CAPACITY);
        log.info("Channel initialized");
    }

    @Test(groups = "channel_write", description = "Successfully write messages to channel")
    public void writeTestSuccess() throws Exception {
        Random random = ThreadLocalRandom.current();
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {

            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY; i++) {
                    testChannel.write(random.nextInt());
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
        Random random = ThreadLocalRandom.current();
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(random.nextInt());
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
        Random random = ThreadLocalRandom.current();
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            Future<?> fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(random.nextInt());
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
        Random random = ThreadLocalRandom.current();
        Future<?> fut = null;
        try (BufferedChannel<Integer> testChannel = new BufferedChannel<>(CAPACITY)) {
            fut = threadPool.submit(() -> {
                for (int i = 0; i < CAPACITY + 1; i++) {
                    testChannel.write(random.nextInt());
                    log.debug("Message id {} written", i);
                }
            });
            TimeUnit.SECONDS.sleep(1);
        }
        try {
            fut.get();
        } catch (ExecutionException ex) {
            log.warn("Exception raised in thread: {}", ex.getCause().getClass());
            throw (Exception) ex.getCause();
        }
    }

    @Test(groups = "channel_read", description = "Reads messages successfully from the channel")
    public void readTestSuccess() throws Exception {

        // Pre-populate the channel
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < CAPACITY; i++) {
            testChannel.write(random.nextInt(100));
        }

        Future<?> fut = threadPool.submit(() -> {
            for (int i = 0; i < CAPACITY; i++) {
                int msg = testChannel.read();
                log.debug("Message read from channel: {}", msg);
                Assert.assertTrue(msg >= 0 && msg < 100);
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
