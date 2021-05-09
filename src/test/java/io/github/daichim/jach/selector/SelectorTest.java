package io.github.daichim.jach.selector;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.ChannelAction;
import io.github.daichim.jach.channel.Selector;
import io.github.daichim.jach.channel.UnbufferedChannel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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
        Channel<String> strChannel = new BufferedChannel<>(5, String.class);
        Channel<Integer> intChannel = new BufferedChannel<>(5, Integer.class);
        Channel<Object> exitChannel = new UnbufferedChannel<>(Object.class);
        return new Channel[] {strChannel, intChannel, exitChannel};
    }

    @Test(timeOut = 2000)
    public void selectTest() {
        Channel[] chans = createChannels();
        AtomicReference<String> expectedChannel = new AtomicReference<>("chan1");
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = Selector.of(
            ChannelAction.action(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Hello");
                Assert.assertEquals(expectedChannel.get(), "chan1");
                expectedChannel.compareAndSet("chan1", "chan2");
                counter.incrementAndGet();
            }),
            ChannelAction.action(chans[1], i->{
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                Assert.assertEquals(expectedChannel.get(), "chan2");
                expectedChannel.compareAndSet("chan2", "chan1");
                counter.incrementAndGet();
            }),
            ChannelAction.action(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i=0; i<10; i++) {
                if (i%2==0) {
                    chans[0].write("Hello");
                } else {
                    chans[1].write(42);
                }
            }
        });
        for (int i=0; i<10; i++) {
            sel.select();
        }
        Assert.assertEquals(counter.get(), 10);
    }

    @Test(timeOut = 4000)
    public void multiThreadSelectTest() throws Exception {
        Channel[] chans = createChannels();
        AtomicInteger counter = new AtomicInteger(0);
        Selector sel = Selector.of(
            ChannelAction.action(chans[0], s -> {
                log.debug("Channel1 read in {}", s);
                Assert.assertEquals(s, "Hello");
                counter.incrementAndGet();
            }),
            ChannelAction.action(chans[1], i->{
                log.debug("Channel2 read in {}", i);
                Assert.assertEquals(i, 42);
                counter.incrementAndGet();
            }),
            ChannelAction.action(chans[2], Selector.BREAK_ACTION)
        );
        threadPool.submit(() -> {
            for (int i=0; i<10; i++) {
                if (i%2==0) {
                    chans[0].write("Hello");
                } else {
                    chans[1].write(42);
                }
            }
        });
        Future<?>[] futures = new Future[10];
        for (int i=0; i<10; i++) {
            futures[i] = threadPool.submit(sel::select);
        }

        for (int i=0; i<10; i++) {
            futures[i].get(1, TimeUnit.SECONDS);
        }
        Assert.assertEquals(counter.get(), 10);
    }


    @AfterClass
    public void cleanup() {
        this.threadPool.shutdownNow();
    }

}
