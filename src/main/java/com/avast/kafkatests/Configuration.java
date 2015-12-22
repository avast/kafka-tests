package com.avast.kafkatests;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configuration {
    public static String redisServer() {
        return "localhost";
    }

    private static String kafkaBrokers() {
        return "localhost:9092";
    }

    public static String kafkaTopic() {
        return "kafka-test";
    }

    public static Duration shutdownTimeout() {
        return Duration.ofSeconds(10);
    }

    public static Properties producerConfiguration() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers());

        return properties;
    }

    public static Properties consumerConfiguration(String groupId, boolean autoCommit) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }

    public static int messagesPerGroup() {
        return 100;
    }

    public static int producerInstances() {
        return 10;
    }

    public static Duration producerSlowDown() {
        return Duration.ofMillis(100);
    }

    public static int consumerInstancesAutoCommit() {
        return 1;
    }

    public static int consumerInstancesSeeking() {
        return 1;
    }

    public static Duration consumerPollTimeout() {
        return Duration.ofSeconds(3);
    }

    public static Duration updateStatePeriod() {
        return Duration.ofSeconds(5);
    }

    public static int checksBeforeFailure() {
        return 15;
    }

    public static int messagesToChangeState() {
        return 100;
    }

    public static int percentFailureProbability() {
        return 40;
    }

    public static List<ConsumerType> consumerTypes() {
        return Arrays.asList(ConsumerType.values());
    }
}
