package com.avast.kafkatests;

public class AutoCommitConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new AutoCommitConsumer(
                configuration.consumerConfiguration("KafkaTestsAutoCommit", true),
                configuration.kafkaTopic(),
                configuration.consumerInstancesAutoCommit(),
                configuration.consumerPollTimeout(),
                configuration.shutdownTimeout(),
                new RedisStateDao(configuration.redisServer()));
    }
}
