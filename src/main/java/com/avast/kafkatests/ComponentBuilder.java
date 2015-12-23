package com.avast.kafkatests;

/**
 * Builder of consumer instances.
 */
public interface ComponentBuilder {
    RunnableComponent newInstance();
}
