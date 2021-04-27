package com.daichim.jach;

import com.daichim.jach.exception.ClosedChannelException;

import java.util.Iterator;

public class UnbufferedChannel<T> extends BufferedChannel<T> {

    public UnbufferedChannel() {
        super(1);
    }

    @Override
    public void write(T data) throws ClosedChannelException {

    }

    @Override
    public T read() throws ClosedChannelException {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }
}
