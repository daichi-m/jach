package io.github.daichim.jach.internal;

/**
 * {@link AfterWriteAction} denotes an action that will be executed in the writer thread once the write is
 * successfully completed to the destination.
 * <p>
 * **NB:** This interface is for internal use only.
 */
@FunctionalInterface
public interface AfterWriteAction {

    void onWrite();

}
