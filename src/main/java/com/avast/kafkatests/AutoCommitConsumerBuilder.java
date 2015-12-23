package com.avast.kafkatests;

public class AutoCommitConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new Kafka09AutoCommitConsumer(
                Configuration.consumerConfiguration("KafkaTestsAutoCommit", true),
                Configuration.kafkaTopic(),
                Configuration.consumerInstancesAutoCommit(),
                Configuration.consumerPollTimeout(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()));
    }
}
