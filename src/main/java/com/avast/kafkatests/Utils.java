package com.avast.kafkatests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility functions.
 */
public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static void logAllUnhandledExceptions() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOGGER.error("Default uncaught exception handler: {}, {}", t, e.toString(), e));
    }

    public static void closeOnShutdown(AutoCloseable closeable) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    LOGGER.info("====================== Shutdown hook ======================");
                    closeable.close();
                } catch (Exception e) {
                    LOGGER.error("Exception while closing on shutdown: {}", e.toString(), e);
                }
            }
        });
    }

    public static void loopWithNoExit() {
        while (true) {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception");
            }

            LOGGER.error("Never ending loop continues");
        }
    }

    public static void shutdownAndWaitTermination(ExecutorService executor, Duration timeout, String name) {
        try {
            LOGGER.debug("Waiting for termination of executor: {}, max {} ms, {}", name, timeout.toMillis(), executor);
            executor.shutdown();

            if (executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                LOGGER.debug("Waiting for termination of executor finished: {}, max {} ms, {}", name, timeout.toMillis(), executor);
            } else {
                LOGGER.error("Waiting for termination of executor timed out: {}, max {} ms, {}", name, timeout.toMillis(), executor);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Waiting for termination of executor interrupted: {}, max {} ms, {}, {}", name, timeout.toMillis(), executor, e.toString(), e);
        }
    }
}
