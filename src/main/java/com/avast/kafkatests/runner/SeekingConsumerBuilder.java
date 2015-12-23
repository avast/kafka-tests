package com.avast.kafkatests.runner;

import com.avast.kafkatests.Configuration;
import com.avast.kafkatests.Kafka09SeekingConsumer;
import com.avast.kafkatests.RedisStateDao;
import com.avast.kafkatests.RunnableComponent;

public class SeekingConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new Kafka09SeekingConsumer(
                Configuration.consumerConfiguration("KafkaTestsSeeking", false),
                Configuration.kafkaTopic(),
                Configuration.consumerInstancesSeeking(),
                Configuration.consumerPollTimeout(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()),
                Configuration.messagesToChangeState(),
                Configuration.percentFailureProbability());
    }
}
