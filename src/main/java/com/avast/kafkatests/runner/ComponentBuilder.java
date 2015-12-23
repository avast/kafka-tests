package com.avast.kafkatests.runner;

import com.avast.kafkatests.RunnableComponent;

/**
 * Builder of consumer instances.
 */
public interface ComponentBuilder {
    RunnableComponent newInstance();
}
