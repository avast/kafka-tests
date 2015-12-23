package com.avast.kafkatests;

public class ProducerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new Kafka09Producer(
                Configuration.producerConfiguration(),
                Configuration.kafkaTopic(),
                Configuration.producerInstances(),
                Configuration.messagesPerGroup(),
                Configuration.producerSlowDown(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()));
    }
}
