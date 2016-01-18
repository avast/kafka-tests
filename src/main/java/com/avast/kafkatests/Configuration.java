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

    private final int chaoticManagerMinComponents;
    private final int chaoticManagerMaxComponents;
    private final int chaoticManagerDecisionsPerUpdate;
    private final Duration chaoticManagerUpdatePeriod;

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

        chaoticManagerMinComponents = config.getInt("chaoticManagerMinComponents");
        chaoticManagerMaxComponents = config.getInt("chaoticManagerMaxComponents");
        chaoticManagerDecisionsPerUpdate = config.getInt("chaoticManagerDecisionsPerUpdate");
        chaoticManagerUpdatePeriod = config.getDuration("chaoticManagerUpdatePeriod");
    }

    public String getRedisServer() {
        return redisServer;
    }

    private String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public Properties producerConfiguration() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBrokers());

        return properties;
    }

    public Properties consumerConfiguration(String groupId, boolean autoCommit) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBrokers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }

    public int getMessagesPerGroup() {
        return messagesPerGroup;
    }

    public int getProducerInstances() {
        return producerInstances;
    }

    public Duration getProducerSlowDown() {
        return producerSlowDown;
    }

    public int getConsumerInstancesAutoCommit() {
        return consumerInstancesAutoCommit;
    }

    public int getConsumerInstancesSeeking() {
        return consumerInstancesSeeking;
    }

    public Duration getConsumerPollTimeout() {
        return consumerPollTimeout;
    }

    public Duration getUpdateStatePeriod() {
        return updateStatePeriod;
    }

    public int getChecksBeforeFailure() {
        return checksBeforeFailure;
    }

    public int getMessagesToChangeState() {
        return messagesToChangeState;
    }

    public int getPercentFailureProbability() {
        return percentFailureProbability;
    }

    public List<ConsumerType> getConsumerTypes() {
        return Arrays.asList(ConsumerType.values());
    }

    public int getChaoticManagerMinComponents() {
        return chaoticManagerMinComponents;
    }

    public int getChaoticManagerMaxComponents() {
        return chaoticManagerMaxComponents;
    }

    public int getChaoticManagerDecisionsPerUpdate() {
        return chaoticManagerDecisionsPerUpdate;
    }

    public Duration getChaoticManagerUpdatePeriod() {
        return chaoticManagerUpdatePeriod;
    }
}
