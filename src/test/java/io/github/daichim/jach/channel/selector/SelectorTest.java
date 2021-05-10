package io.github.daichim.jach.channel.selector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.UnbufferedChannel;
import io.github.daichim.jach.channel.copier.RefCopier;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.daichim.jach.JachChannels.selectCase;
import static io.github.daichim.jach.JachChannels.selector;

@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class SelectorTest {

    ExecutorService threadPool;

    @BeforeClass
    public void setup() {
        this.threadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("test-thread-%d").build());
    }

    private Channel[] createChannels() {
        Channel<String> strChannel = new BufferedChannel<>(5, String.class, new RefCopier<>());
        Channel<Integer> intChannel = new BufferedChannel<>(5, Integer.class, new RefCopier<>());
        Channel<Object> exitChannel = new UnbufferedChannel<>(Object.class);
        return new Channel[]{strChannel, intChannel, exitChannel};
    }

    @Test(timeOut = 2000)
    public void selectTest() throws Exception {
        Channel[] chans = createChannels();
        AtomicReference<String> expectedChannel = new AtomicReference<>("chan1");
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Hello");
                Assert.assertEquals(expectedChannel.get(), "chan1");
                expectedChannel.compareAndSet("chan1", "chan2");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                Assert.assertEquals(expectedChannel.get(), "chan2");
                expectedChannel.compareAndSet("chan2", "chan1");
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    chans[0].write("Hello");
                } else {
                    chans[1].write(42);
                }
            }
        });
        for (int i = 0; i < 10; i++) {
            sel.select();
        }
        Assert.assertEquals(counter.get(), 10);
        Arrays.stream(chans).forEach(ch -> ch.close());
        TimeUnit.MILLISECONDS.sleep(200);
        for (int i = 0; i < chans.length; i++) {
            sel.select();
        }
        Assert.assertFalse(sel.isActive());
    }

    @Test(timeOut = 4000)
    public void multiThreadSelectTest() throws Exception {
        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Hello");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    chans[0].write("Hello");
                } else {
                    chans[1].write(42);
                }
            }
            chans[2].write(new Object());
        });
        Future<?>[] futures = new Future[10];
        for (int i = 0; i < 10; i++) {
            futures[i] = threadPool.submit(sel::select);
        }

        for (int i = 0; i < 10; i++) {
            futures[i].get(1, TimeUnit.SECONDS);
        }
        Assert.assertEquals(counter.get(), 10);
        Arrays.stream(chans).forEach(ch -> ch.close());
        TimeUnit.MILLISECONDS.sleep(200);
        for (int i = 0; i < chans.length; i++) {
            sel.select();
        }
        Assert.assertFalse(sel.isActive());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void selectAfterCloseTest() {
        Channel<String> chan = new BufferedChannel<>(10, String.class, new RefCopier<>());
        Selector selector = selector(selectCase(chan, s -> {}));
        selector.close();
        selector.select();
    }

    @Test(timeOut = 10_000)
    public void selectorOverflowTest() throws Exception {
        Channel<String> bigChannel = new BufferedChannel<>(10*1024, String.class, new RefCopier<>());
        Channel<Object> exitChannel = new UnbufferedChannel<>(Object.class, new RefCopier<>());
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(bigChannel, s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Hello");
                counter.incrementAndGet();
            }),
            selectCase(exitChannel, Selector.BREAK_ACTION)
        );
        Future<?> fut = threadPool.submit(() -> {
            for (int i = 0; i < 8192; i++) {
                bigChannel.write("Hello");
            }
            exitChannel.write(new Object());
        });
        fut.get();
        sel.untilDone();
        Assert.assertEquals(counter.get(), 2047);
    }

    @Test
    public void untilDoneTest() {
        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Universe");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 100; i++) {
                boolean c = ThreadLocalRandom.current().nextBoolean();
                if (c) {
                    chans[0].write("Universe");
                } else {
                    chans[1].write(42);
                }
            }
            chans[2].write("EXIT");
        });
        sel.untilDone();
        Assert.assertEquals(counter.get(), 100);
        Assert.assertFalse(sel.isActive());
    }


    @Test
    public void untilDoneCloseChannelsTest() {
        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Universe");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 100; i++) {
                boolean c = ThreadLocalRandom.current().nextBoolean();
                if (c) {
                    chans[0].write("Universe");
                } else {
                    chans[1].write(42);
                }
            }
            chans[0].close();
            chans[1].close();
            chans[2].close();
        });
        sel.untilDone();
        Assert.assertEquals(counter.get(), 100);
        Assert.assertFalse(sel.isActive());
    }

    @Test
    public void untilDoneOrDefaultTest() {

        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger defaultCalled = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Universe");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 100; i++) {
                boolean c = ThreadLocalRandom.current().nextBoolean();
                if (c) {
                    chans[0].write("Universe");
                } else {
                    chans[1].write(42);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            chans[2].write("EXIT");
        });
        sel.untilOrDefault(() -> {
            log.debug("Default action invoked");
            defaultCalled.incrementAndGet();
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        });
        Assert.assertEquals(counter.get(), 100);
        Assert.assertFalse(sel.isActive());
        Assert.assertTrue(defaultCalled.get() >= 20);
    }


    @Test
    public void untilDoneOrDefaultCloseChannelsTest() {

        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger defaultCalled = new AtomicInteger(0);
        Selector sel = selector(
            selectCase(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Universe");
                counter.incrementAndGet();
            }),
            selectCase(chans[1], i -> {
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            selectCase(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i = 0; i < 100; i++) {
                boolean c = ThreadLocalRandom.current().nextBoolean();
                if (c) {
                    chans[0].write("Universe");
                } else {
                    chans[1].write(42);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
            chans[0].close();
            chans[1].close();
            chans[2].close();
        });
        sel.untilOrDefault(() -> {
            log.debug("Default action invoked");
            defaultCalled.incrementAndGet();
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        });
        Assert.assertEquals(counter.get(), 100);
        Assert.assertFalse(sel.isActive());
        Assert.assertTrue(defaultCalled.get() >= 20);
    }


    @AfterClass
    public void cleanup() {
        this.threadPool.shutdownNow();
    }

}
