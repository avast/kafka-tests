package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Consumer of test messages from Kafka with auto commit.
 */
public class Kafka09AutoCommitConsumer implements AutoCloseable, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Kafka09AutoCommitConsumer.class);

    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Properties configuration;
    private final String topic;
    private final Duration pollTimeout;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;

    public Kafka09AutoCommitConsumer(Properties configuration, String topic, int instances, Duration pollTimeout, Duration shutdownTimeout, StateDao stateDao) {
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
            consumer.subscribe(Collections.singletonList(topic), new RebalanceCallback());

            while (!finish.get()) {
                ConsumerRecords<String, Integer> records = consumer.poll(pollTimeout.toMillis());

                for (ConsumerRecord<String, Integer> record : records) {
                    LOGGER.trace("Message consumed: {}, {}, {}/{}/{}", record.key(), record.value(), record.topic(), record.partition(), record.offset());
                    stateDao.markConsume(ConsumerType.autocommit, UUID.fromString(record.key()), record.value());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred: {}", e, e);
        }

        LOGGER.info("Worker thread stopped");
    }

    private static class RebalanceCallback implements ConsumerRebalanceListener {
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
            LOGGER.info("Rebalance callback, revoked: {}", partitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
            LOGGER.info("Rebalance callback, assigned: {}", partitions);
        }
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();

        Kafka09AutoCommitConsumer instance = new Kafka09AutoCommitConsumer(
                Configuration.consumerConfiguration("KafkaTestsAutoCommit", true),
                Configuration.kafkaTopic(),
                Configuration.consumerInstancesAutoCommit(),
                Configuration.consumerPollTimeout(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()));

        Utils.closeOnShutdown(instance);
        Utils.loopWithNoExit();
    }
}
