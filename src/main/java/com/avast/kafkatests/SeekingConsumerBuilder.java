package com.avast.kafkatests;

public class SeekingConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new SeekingConsumer(
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
