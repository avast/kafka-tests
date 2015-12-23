package com.avast.kafkatests.runner;

import com.avast.kafkatests.Configuration;
import com.avast.kafkatests.Kafka09AutoCommitConsumer;
import com.avast.kafkatests.RedisStateDao;
import com.avast.kafkatests.RunnableComponent;

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
