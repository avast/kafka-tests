package com.avast.kafkatests;

public class AutoCommitConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new AutoCommitConsumer(
                configuration.consumerConfiguration("KafkaTestsAutoCommit", true),
                configuration.getKafkaTopic(),
                configuration.getConsumerInstancesAutoCommit(),
                configuration.getConsumerPollTimeout(),
                configuration.getShutdownTimeout(),
                new RedisStateDao(configuration.getRedisServer()));
    }
}
