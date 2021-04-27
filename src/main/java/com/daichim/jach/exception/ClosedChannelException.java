package com.daichim.jach.exception;

public class ClosedChannelException extends RuntimeException {

    private static final long serialVersionUID = -1981876009862505320L;

    public ClosedChannelException() {
        super();
    }

    public ClosedChannelException(String message) {
        super(message);
    }
}
