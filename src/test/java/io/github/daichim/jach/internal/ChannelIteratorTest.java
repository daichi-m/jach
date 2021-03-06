package io.github.daichim.jach.internal;

import io.github.daichim.jach.channel.BufferedChannel;
import io.github.daichim.jach.exception.ClosedChannelException;
import io.github.daichim.jach.exception.NoSuchChannelElementException;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.testng.Assert;
import org.testng.annotations.Test;

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

    @Test(description = "Check next after channel is closed but has data")
    public void testNextClosedChannel() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);
        Mockito.doReturn(LIFE_UNIVERSE_AND_EVERYTHING)
            .doReturn(LIFE_UNIVERSE_AND_EVERYTHING)
            .doThrow(NoSuchChannelElementException.class)
            .when(channel).read();
        Mockito.doReturn(true)
            .doReturn(true)
            .doReturn(false)
            .when(channel).canRead();

        iterator.markDone();

        int msg = iterator.next();
        Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
        msg = iterator.next();
        Assert.assertEquals(msg, LIFE_UNIVERSE_AND_EVERYTHING);
        try {
            iterator.next();
            Assert.fail("Should have failed in next since no more element in channel");
        } catch (NoSuchChannelElementException ex) {
            Assert.assertTrue(true);
        }
    }

    @Test(description = "Check next after channel is closed but has no  data")
    public void testNextClosedChannelNoData() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);
        Mockito.doThrow(NoSuchChannelElementException.class)
            .when(channel).read();
        Mockito.doReturn(false)
            .when(channel).canRead();

        iterator.markDone();
        try {
            iterator.next();
            Assert.fail("Should have failed in next since no more element in channel");
        } catch (NoSuchChannelElementException ex) {
            Assert.assertTrue(true);
        }
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

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void removeTest() {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);
        iterator.remove();
    }


    @Test(enabled = true)
    public void forEachRemainingTest() throws Exception {
        BufferedChannel<Integer> channel = Mockito.mock(BufferedChannel.class);
        ChannelIterator<Integer> iterator = new ChannelIterator<>(channel);

        int readEarly = 15;
        Stubber stubber = Mockito.doReturn(0);
        for (int i=1; i<25; i++) {
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
        Assert.assertEquals(actionCtr.intValue(), 25 - readEarly);
        log.info("For each remaining action called for {} messages", (100 - readEarly));
    }

}
