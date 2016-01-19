package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Consumer of test messages from Kafka with seeking and rewinding to the last committed position on error.
 */
public class SeekingConsumer extends AbstractComponent {
    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Properties configuration;
    private final String topic;
    private final Duration pollTimeout;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;
    private final int messagesToChangeState;
    private final int percentFailureProbability;

    public SeekingConsumer(Properties configuration, String topic, int instances, Duration pollTimeout,
                           Duration shutdownTimeout, StateDao stateDao,
                           int messagesToChangeState, int percentFailureProbability) {
        logger.info("Starting instance");

        this.configuration = configuration;
        this.topic = topic;
        this.pollTimeout = pollTimeout;
        this.shutdownTimeout = shutdownTimeout;
        this.stateDao = stateDao;
        this.messagesToChangeState = messagesToChangeState;
        this.percentFailureProbability = percentFailureProbability;

        if (pollTimeout.toMillis() * 2 > shutdownTimeout.toMillis()) {
            throw new IllegalArgumentException("Shutdown timeout must be at least twice bigger than poll timeout");
        }

        this.executor = Executors.newFixedThreadPool(instances,
                new ThreadFactoryBuilder()
                        .setNameFormat(getClass().getSimpleName() + "-" + instanceName + "-worker-" + "%d")
                        .setDaemon(false)
                        .build());

        IntStream.range(0, instances)
                .forEach((v) -> executor.submit(this));
    }

    @Override
    public void close() {
        logger.info("Closing instance");
        finish.set(true);

        Utils.shutdownAndWaitTermination(executor, shutdownTimeout, getClass().getSimpleName() + "-" + instanceName);
        stateDao.close();
    }

    @Override
    public void run() {
        logger.info("Worker thread started");

        try (Consumer<String, Integer> consumer = new KafkaConsumer<>(configuration, new StringDeserializer(), new IntegerDeserializer())) {
            SeekingConsumerLogic logic = new SeekingConsumerLogic(consumer, stateDao, messagesToChangeState, percentFailureProbability);
            consumer.subscribe(Collections.singletonList(topic), logic);

            while (!finish.get()) {
                ConsumerRecords<String, Integer> records = consumer.poll(pollTimeout.toMillis());

                long startTime = System.nanoTime();
                logic.processMessages(records);
                long duration = System.nanoTime() - startTime;

                logger.debug("Processing of poll batch finished: {} messages, {} ms", records.count(), TimeUnit.NANOSECONDS.toMillis(duration));
            }

            logic.optionallyCommitAllOffsets();
        } catch (Exception e) {
            logger.error("Unexpected exception occurred: {}", e, e);
        }

        logger.info("Worker thread stopped");
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();
        Utils.closeOnShutdown(new SeekingConsumerBuilder().newInstance());
        Utils.loopWithNoExit();
    }
}
