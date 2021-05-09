package io.github.daichim.jach.channel;

import java.util.function.Consumer;

/**
 * {@link Action} is a void consumer. It does not take any parameters and does not return any
 * value.
 */
@FunctionalInterface
public interface Action extends Consumer<Void> {

    @Override
    default void accept(Void unused) {
        accept();
    }

    void accept();
}
