package io.github.daichim.jach;

import io.github.daichim.jach.exception.ClosedChannelException;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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

    @BeforeClass
    public void setupClass() {
        threadPool = Executors.newCachedThreadPool();
    }

    @Test
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

    @Test(expectedExceptions = TimeoutException.class)
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

    @Test
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
            } catch (TimeoutException ex) {
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

    @Test(expectedExceptions = ClosedChannelException.class)
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
            throw (Exception) ex.getCause();
        }
    }


    @AfterClass
    public void cleanupClass() {
        threadPool.shutdownNow();
    }
}
