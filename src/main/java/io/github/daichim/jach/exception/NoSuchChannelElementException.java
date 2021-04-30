package io.github.daichim.jach.exception;

import io.github.daichim.jach.Channel;

import java.util.NoSuchElementException;

/**
 * Exception thrown when the channel is closed while iterating over a {@link
 * Channel}. It is thrown by the channel's iterator, but using forEach method would
 * not propagate this exception to the caller.
 */
public class NoSuchChannelElementException extends NoSuchElementException {

    private static final long serialVersionUID = -6531794534106851223L;

    public NoSuchChannelElementException() {
        super();
    }
}
