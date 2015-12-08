package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Consumer of test messages from Kafka with seeking and rewinding to the last committed position on error.
 */
public class Kafka09SeekingConsumer implements AutoCloseable, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Kafka09SeekingConsumer.class);

    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Properties configuration;
    private final String topic;
    private final Duration pollTimeout;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;
    private final int messagesToChangeState;
    private final int percentFailureProbability;

    public Kafka09SeekingConsumer(Properties configuration, String topic, int instances, Duration pollTimeout,
                                  Duration shutdownTimeout, StateDao stateDao,
                                  int messagesToChangeState, int percentFailureProbability) {
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
                        .setNameFormat(getClass().getSimpleName() + "-worker-%d")
                        .setDaemon(false)
                        .build());

        IntStream.range(0, instances)
                .forEach((v) -> executor.submit(this));
    }

    @Override
    public void close() {
        LOGGER.info("Closing instance");
        finish.set(true);

        Utils.shutdownAndWaitTermination(executor, shutdownTimeout, getClass().getSimpleName());
        stateDao.close();
    }

    @Override
    public void run() {
        LOGGER.info("Worker thread started");

        try (Consumer<String, Integer> consumer = new KafkaConsumer<>(configuration, new StringDeserializer(), new IntegerDeserializer())) {
            SeekingConsumerLogic logic = new SeekingConsumerLogic(consumer, stateDao, messagesToChangeState, percentFailureProbability);
            consumer.subscribe(Collections.singletonList(topic), logic);

            while (!finish.get()) {
                ConsumerRecords<String, Integer> records = consumer.poll(pollTimeout.toMillis());
                logic.processMessages(records);
            }

            logic.optionallyCommitAllOffsets();
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred: {}", e, e);
        }

        LOGGER.info("Worker thread stopped");
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();

        Kafka09SeekingConsumer instance = new Kafka09SeekingConsumer(
                Configuration.consumerConfiguration("KafkaTestsSeeking", false),
                Configuration.kafkaTopic(),
                Configuration.consumerInstancesSeeking(),
                Configuration.consumerPollTimeout(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()),
                Configuration.messagesToChangeState(),
                Configuration.percentFailureProbability());

        Utils.closeOnShutdown(instance);
        Utils.loopWithNoExit();
    }
}
