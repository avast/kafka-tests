package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Chaotic manager that periodically and randomly changes previous decisions, starts and stops components.
 */
public class ChaoticManager implements RunnableComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaoticManager.class);

    private final ScheduledExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Duration shutdownTimeout;

    private final List<RunnableComponent> activeComponents = new ArrayList<>();
    private final ComponentBuilder componentBuilder;
    private final int minComponents; // Inclusive
    private final int maxComponents; // Inclusive
    private final int decisionsPerUpdate;

    private final Random random = new Random();

    public ChaoticManager(ComponentBuilder componentBuilder, int minComponents, int maxComponents, int decisionsPerUpdate,
                          Duration updatePeriod, Duration shutdownTimeout) {
        this.componentBuilder = componentBuilder;
        this.minComponents = minComponents;
        this.maxComponents = maxComponents;
        this.decisionsPerUpdate = decisionsPerUpdate;
        this.shutdownTimeout = shutdownTimeout;

        IntStream.range(0, minComponents).forEach(i -> startNewComponent());

        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(getClass().getSimpleName() + "-timer-%d")
                .setDaemon(false)
                .build());

        executor.scheduleWithFixedDelay(this, 0, updatePeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        LOGGER.info("Closing instance");
        finish.set(true);

        Utils.shutdownAndWaitTermination(executor, shutdownTimeout, getClass().getSimpleName());

        LOGGER.info("Stopping all components");
        activeComponents.forEach(RunnableComponent::close);
        activeComponents.clear();
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Making some random decisions");
            IntStream.range(0, decisionsPerUpdate).forEach(i -> randomDecision());
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred: {}", e, e);
        }
    }

    private void randomDecision() {
        int type = random.nextInt() % 3;

        switch (type) {
            case 0:
                // Don't know, no decision and no change.
                break;

            case 1:
                stopRandomComponent();
                break;

            case 2:
                startNewComponent();
                break;

            default:
                LOGGER.error("Unsupported decision type: {}", type);
                break;
        }
    }

    private void stopRandomComponent() {
        if (activeComponents.size() <= minComponents) {
            return;
        }

        int index = random.nextInt(activeComponents.size());
        LOGGER.info("Stopping a random component: {}", index);

        RunnableComponent component = activeComponents.remove(index);
        component.close();
    }

    private void startNewComponent() {
        if (activeComponents.size() >= maxComponents) {
            return;
        }

        LOGGER.info("Starting a new component: {}", activeComponents.size());
        activeComponents.add(componentBuilder.newInstance());
    }
}
