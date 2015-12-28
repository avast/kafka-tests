package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

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
public class GeneratorProducer extends AbstractComponent {
    private final ExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final String topic;
    private final Producer<String, Integer> producer;
    private final int messagesPerGroup;
    private final Duration producerSlowDown;
    private final Duration shutdownTimeout;
    private final StateDao stateDao;

    public GeneratorProducer(Properties configuration, String topic, int instances, int messagesPerGroup,
                             Duration producerSlowDown, Duration shutdownTimeout,
                             StateDao stateDao) {
        logger.info("Starting instance");

        this.topic = topic;
        this.messagesPerGroup = messagesPerGroup;
        this.producerSlowDown = producerSlowDown;
        this.shutdownTimeout = shutdownTimeout;
        this.stateDao = stateDao;

        this.producer = new KafkaProducer<>(configuration, new StringSerializer(), new IntegerSerializer());

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
        producer.close(shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS);
        stateDao.close();
    }

    @Override
    public void run() {
        logger.info("Worker thread started");

        try {
            while (!finish.get()) {
                produceMessagesGroup();
            }
        } catch (Exception e) {
            logger.error("Unexpected exception occurred: {}", e, e);
        }

        logger.info("Worker thread stopped");
    }

    private void produceMessagesGroup() {
        UUID key = UUID.randomUUID();
        logger.debug("Starting message group: {} topic, {}", topic, key);
        IntStream.range(0, messagesPerGroup).forEach((value) -> produceSingleMessage(key, value));
    }

    private void produceSingleMessage(UUID key, int value) {
        logger.trace("Producing message: {} topic, {}, {}", topic, key, value);

        stateDao.markSend(key, value);
        producer.send(new ProducerRecord<>(topic, key.toString(), value),
                (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Producing message failed: {}", exception, exception);
                        stateDao.markSendFail(key, value);
                    } else {
                        logger.trace("Producing message successful: {} topic, {}, {}", topic, key, value);
                        stateDao.markSendConfirm(key, value);
                    }
                });

        if (!finish.get() && producerSlowDown.toMillis() > 0) {
            try {
                Thread.sleep(producerSlowDown.toMillis());
            } catch (InterruptedException e) {
                logger.error("Interrupted exception in producer");
            }
        }
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();
        Utils.closeOnShutdown(new GeneratorProducerBuilder().newInstance());
        Utils.loopWithNoExit();
    }
}
