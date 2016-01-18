package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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

        executor.scheduleWithFixedDelay(this, updatePeriod.toMillis(), updatePeriod.toMillis(), TimeUnit.MILLISECONDS);
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
        int type = random.nextInt(2);

        switch (type) {
            case 0:
                stopRandomComponent();
                break;

            case 1:
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

    private static void usage() {
        String components = Arrays.stream(ComponentType.values())
                .map(Enum::toString)
                .collect(Collectors.joining(", "));

        LOGGER.info("Usage: {} componentType minComponents maxComponents decisionsPerUpdate updatePeriodMs", ChaoticManager.class.getSimpleName());
        LOGGER.info("Usage: Supported component types: {}", components);
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            usage();
            LOGGER.error("Not enough arguments");
            System.exit(1);
        }

        try {
            Configuration configuration = new Configuration();

            ComponentType componentType = ComponentType.valueOf(args[0]);
            int minComponents = Integer.parseInt(args[1]);
            int maxComponents = Integer.parseInt(args[2]);
            int decisionsPerUpdate = Integer.parseInt(args[3]);
            Duration updatePeriod = Duration.ofMillis(Long.parseLong(args[4]));

            Utils.logAllUnhandledExceptions();
            Utils.closeOnShutdown(new ChaoticManager(componentType.getComponentBuilder(),
                    minComponents, maxComponents, decisionsPerUpdate, updatePeriod, configuration.shutdownTimeout()));
            Utils.loopWithNoExit();
        } catch (NumberFormatException e) {
            usage();
            LOGGER.error("Parsing of argument failed: {}", e.toString());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            usage();
            LOGGER.error("Unsupported component type: {}", args[0]);
            System.exit(1);
        }
    }

    private enum ComponentType {
        GeneratorProducer(new GeneratorProducerBuilder()),
        AutoCommitConsumer(new AutoCommitConsumerBuilder()),
        SeekingConsumer(new SeekingConsumerBuilder());

        private final ComponentBuilder componentBuilder;

        ComponentType(ComponentBuilder componentBuilder) {
            this.componentBuilder = componentBuilder;
        }

        public ComponentBuilder getComponentBuilder() {
            return componentBuilder;
        }
    }
}
