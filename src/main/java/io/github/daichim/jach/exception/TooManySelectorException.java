package io.github.daichim.jach.exception;

import io.github.daichim.jach.channel.Channel;
import io.github.daichim.jach.channel.selector.Selector;

/**
 * {@link TooManySelectorException} is thrown when a particular {@link
 * Channel} is used in too many
 * {@link Selector}s
 * and the system cannot sustain it.
 */
public class TooManySelectorException extends RuntimeException {

    private static final long serialVersionUID = 3055385757957172482L;

    public TooManySelectorException() {
    }

    public TooManySelectorException(String message) {
        super(message);
    }

    public TooManySelectorException(String message, Throwable cause) {
        super(message, cause);
    }
}
