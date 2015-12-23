package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Producer of test messages to Kafka.
 */
public class Kafka09Producer implements RunnableComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Kafka09Producer.class);

    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final String topic;
    private final Producer<String, Integer> producer;
    private final int messagesPerGroup;
    private final Duration producerSlowDown;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;

    public Kafka09Producer(Properties configuration, String topic, int instances, int messagesPerGroup,
                           Duration producerSlowDown, Duration shutdownTimeout,
                           StateDao stateDao) {
        this.topic = topic;
        this.messagesPerGroup = messagesPerGroup;
        this.producerSlowDown = producerSlowDown;
        this.shutdownTimeout = shutdownTimeout;
        this.stateDao = stateDao;

        this.producer = new KafkaProducer<>(configuration, new StringSerializer(), new IntegerSerializer());

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
        producer.close(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
        stateDao.close();
    }

    @Override
    public void run() {
        LOGGER.info("Worker thread started");

        try {
            while (!finish.get()) {
                produceMessagesGroup();
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred: {}", e, e);
        }

        LOGGER.info("Worker thread stopped");
    }

    private void produceMessagesGroup() {
        UUID key = UUID.randomUUID();
        LOGGER.debug("Starting message group: {} topic, {}", topic, key);
        IntStream.range(0, messagesPerGroup).forEach((value) -> produceSingleMessage(key, value));
    }

    private void produceSingleMessage(UUID key, int value) {
        LOGGER.trace("Producing message: {} topic, {}, {}", topic, key, value);

        stateDao.markSend(key, value);
        producer.send(new ProducerRecord<>(topic, key.toString(), value),
                (metadata, exception) -> {
                    if (exception != null) {
                        LOGGER.error("Producing message failed: {}", exception, exception);
                        stateDao.markSendFail(key, value);
                    } else {
                        LOGGER.trace("Producing message successful: {} topic, {}, {}", topic, key, value);
                        stateDao.markSendConfirm(key, value);
                    }
                });

        if (!finish.get() && producerSlowDown.toMillis() > 0) {
            try {
                Thread.sleep(producerSlowDown.toMillis());
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted exception in producer");
            }
        }
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();
        Utils.closeOnShutdown(new ProducerBuilder().newInstance());
        Utils.loopWithNoExit();
    }
}
