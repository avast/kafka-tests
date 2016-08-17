package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Consumer of test messages from Kafka with auto commit.
 */
public class AutoCommitConsumer extends AbstractComponent implements ConsumerRebalanceListener {
    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Properties configuration;
    private final String topic;
    private final Duration pollTimeout;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;

    public AutoCommitConsumer(Properties configuration, String topic, int instances, Duration pollTimeout, Duration shutdownTimeout, StateDao stateDao) {
        logger.info("Starting instance");

        this.configuration = configuration;
        this.topic = topic;
        this.pollTimeout = pollTimeout;
        this.shutdownTimeout = shutdownTimeout;
        this.stateDao = stateDao;

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
            consumer.subscribe(Collections.singletonList(topic), this);

            while (!finish.get()) {
                ConsumerRecords<String, Integer> records = consumer.poll(pollTimeout.toMillis());

                long startTime = System.nanoTime();
                for (ConsumerRecord<String, Integer> record : records) {
                    logger.trace("Message consumed: {}, {}, {}-{}/{}", record.key(), record.value(), record.topic(), record.partition(), record.offset());
                    stateDao.markConsume(ConsumerType.autocommit, UUID.fromString(record.key()), record.value());
                }
                long duration = System.nanoTime() - startTime;

                logger.debug("Processing of poll batch finished: {} messages, {} ms", records.count(), TimeUnit.NANOSECONDS.toMillis(duration));
            }
        } catch (Exception e) {
            logger.error("Unexpected exception occurred: {}", e.toString(), e);
        }

        logger.info("Worker thread stopped");
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        logger.info("Rebalance callback, revoked: {}", partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        logger.info("Rebalance callback, assigned: {}", partitions);
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();
        Utils.closeOnShutdown(new AutoCommitConsumerBuilder().newInstance());
        Utils.loopWithNoExit();
    }
}
