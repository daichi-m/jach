package io.github.daichim.jach.exception;

public class CopyException extends RuntimeException {
    public CopyException() {
    }

    public CopyException(String message) {
        super(message);
    }

    public CopyException(String message, Throwable cause) {
        super(message, cause);
    }
}
