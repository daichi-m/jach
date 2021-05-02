package io.github.daichim.jach.internal;

import io.github.daichim.jach.BufferedChannel;
import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.swing.Action;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class ChannelIteratorTest {

    public static final int LIFE_UNIVERSE_AND_EVERYTHING = 42;

    @Test(description = "Check next element")
    public void testNext() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);

        Mockito.doReturn(LIFE_UNIVERSE_AND_EVERYTHING)
            .doReturn(LIFE_UNIVERSE_AND_EVERYTHING)
            .when(channel).read();

        int msg = iterator.next();
        Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
        msg = iterator.next();
        Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
        log.info("Iterator next worked successfully");
    }

    @Test(description = "Check next element after an NPE")
    public void testNextNull() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);

        Mockito.doThrow(NullPointerException.class)
            .doReturn(LIFE_UNIVERSE_AND_EVERYTHING)
            .when(channel).read();
        int msg = iterator.next();
        Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
        log.info("Iterator read in next element after NPE");
    }

    @Test(expectedExceptions = NoSuchChannelElementException.class,
        description = "Next on a closed channel")
    public void testNextNoSuchElement() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);

        Mockito.doThrow(ClosedChannelException.class)
            .when(channel).read();
        int msg = iterator.next();
    }


    @Test(enabled = true)
    public void forEachRemainingTest() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);

        int readEarly = 20;
        Stubber stubber = Mockito.doReturn(0);
        for (int i=1; i<100; i++) {
            stubber.doReturn(i);
        }
        stubber.doThrow(ClosedChannelException.class).when(channel).read();

        AtomicInteger actionCtr = new AtomicInteger(0);
        Consumer<Integer> action = msg -> {
            Assert.assertTrue(msg >= 0 && msg < 100);
            log.debug("Message read in {}", msg);
            actionCtr.incrementAndGet();
        };

        for (int i=0; i<readEarly; i++) {
            int msg = iterator.next();
            Assert.assertTrue(msg >= 0 && msg < 100);
        }
        log.info("Read in {} messages", readEarly);

        iterator.forEachRemaining(action);
        Assert.assertEquals(actionCtr.intValue(), 100 - readEarly);
        log.info("For each remaining action called for {} messages", (100 - readEarly));
    }

}
