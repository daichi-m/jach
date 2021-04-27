package com.daichim.jach.internal;

import com.daichim.jach.BufferedChannel;
import com.daichim.jach.exception.ClosedChannelException;
import com.daichim.jach.exception.NoSuchChannelElementException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;


public class BufferedChannelIterator<T> implements Iterator<T> {

    private final BufferedChannel<T> parentChannel;
    private volatile boolean done;

    public BufferedChannelIterator(BufferedChannel<T> parentChannel) {
        this.parentChannel = parentChannel;
        this.done = false;
    }

    @Override
    synchronized public boolean hasNext() {
        return !this.done;
    }

    @Override
    public T next() throws NoSuchChannelElementException {
        if (!hasNext()) {
            throw new NoSuchChannelElementException();
        }
        try {
            return parentChannel.read();
        } catch (ClosedChannelException | IllegalStateException ex) {
            this.done = true;
            throw new NoSuchElementException();
        } catch (NullPointerException ex) {
            return next();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from channel");
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        try {
            while (this.hasNext()) {
                T val = this.next();
                action.accept(val);
            }
        } catch (NoSuchElementException ex) {}
    }

    synchronized public void markDone() {
        this.done = true;
    }
}
