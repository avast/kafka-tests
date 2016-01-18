package com.avast.kafkatests;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configuration {
    /**
     * Name of root configuration object.
     */
    private static final String CFG_ROOT = "kafka-tests";

    private final String redisServer;
    private final String kafkaBrokers;
    private final String kafkaTopic;
    private final Duration shutdownTimeout;
    private final int messagesPerGroup;
    private final int producerInstances;
    private final Duration producerSlowDown;
    private final int consumerInstancesAutoCommit;
    private final int consumerInstancesSeeking;
    private final Duration consumerPollTimeout;
    private final Duration updateStatePeriod;
    private final int checksBeforeFailure;
    private final int messagesToChangeState;
    private final int percentFailureProbability;

    public Configuration() {
        Config config = ConfigFactory.load().getConfig(CFG_ROOT);

        redisServer = config.getString("redisServer");
        kafkaBrokers = config.getString("kafkaBrokers");
        kafkaTopic = config.getString("kafkaTopic");
        shutdownTimeout = config.getDuration("shutdownTimeout");
        messagesPerGroup = config.getInt("messagesPerGroup");
        producerInstances = config.getInt("producerInstances");
        producerSlowDown = config.getDuration("producerSlowDown");
        consumerInstancesAutoCommit = config.getInt("consumerInstancesAutoCommit");
        consumerInstancesSeeking = config.getInt("consumerInstancesSeeking");
        consumerPollTimeout = config.getDuration("consumerPollTimeout");
        updateStatePeriod = config.getDuration("updateStatePeriod");
        checksBeforeFailure = config.getInt("checksBeforeFailure");
        messagesToChangeState = config.getInt("messagesToChangeState");
        percentFailureProbability = config.getInt("percentFailureProbability");
    }

    public String redisServer() {
        return redisServer;
    }

    private String kafkaBrokers() {
        return kafkaBrokers;
    }

    public String kafkaTopic() {
        return kafkaTopic;
    }

    public Duration shutdownTimeout() {
        return shutdownTimeout;
    }

    public Properties producerConfiguration() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers());

        return properties;
    }

    public Properties consumerConfiguration(String groupId, boolean autoCommit) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }

    public int messagesPerGroup() {
        return messagesPerGroup;
    }

    public int producerInstances() {
        return producerInstances;
    }

    public Duration producerSlowDown() {
        return producerSlowDown;
    }

    public int consumerInstancesAutoCommit() {
        return consumerInstancesAutoCommit;
    }

    public int consumerInstancesSeeking() {
        return consumerInstancesSeeking;
    }

    public Duration consumerPollTimeout() {
        return consumerPollTimeout;
    }

    public Duration updateStatePeriod() {
        return updateStatePeriod;
    }

    public int checksBeforeFailure() {
        return checksBeforeFailure;
    }

    public int messagesToChangeState() {
        return messagesToChangeState;
    }

    public int percentFailureProbability() {
        return percentFailureProbability;
    }

    public List<ConsumerType> consumerTypes() {
        return Arrays.asList(ConsumerType.values());
    }
}
