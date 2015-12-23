package com.avast.kafkatests;

/**
 * Abstract runnable component.
 */
public interface RunnableComponent extends AutoCloseable, Runnable {
    @Override
    void close();
}
