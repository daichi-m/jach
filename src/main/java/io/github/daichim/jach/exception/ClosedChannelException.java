package io.github.daichim.jach.exception;

import io.github.daichim.jach.channel.Channel;

/**
 * Exception thrown in case of attempting to operate on a {@link Channel} which has
 * already been closed.
 */
public class ClosedChannelException extends RuntimeException {

    private static final long serialVersionUID = -1981876009862505320L;

    public ClosedChannelException() {
        super();
    }

    public ClosedChannelException(String message) {
        super(message);
    }
}
