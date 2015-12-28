package com.avast.kafkatests;

/**
 * Builder of worker components.
 */
public interface ComponentBuilder {
    RunnableComponent newInstance();
}
