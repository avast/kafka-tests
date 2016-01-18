package com.avast.kafkatests;

public class SeekingConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new SeekingConsumer(
                configuration.consumerConfiguration("KafkaTestsSeeking", false),
                configuration.kafkaTopic(),
                configuration.consumerInstancesSeeking(),
                configuration.consumerPollTimeout(),
                configuration.shutdownTimeout(),
                new RedisStateDao(configuration.redisServer()),
                configuration.messagesToChangeState(),
                configuration.percentFailureProbability());
    }
}
