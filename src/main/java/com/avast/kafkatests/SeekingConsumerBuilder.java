package com.avast.kafkatests;

public class SeekingConsumerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new SeekingConsumer(
                configuration.consumerConfiguration("KafkaTestsSeeking", false),
                configuration.getKafkaTopic(),
                configuration.getConsumerInstancesSeeking(),
                configuration.getConsumerPollTimeout(),
                configuration.getShutdownTimeout(),
                new RedisStateDao(configuration.getRedisServer()),
                configuration.getMessagesToChangeState(),
                configuration.getPercentFailureProbability());
    }
}
