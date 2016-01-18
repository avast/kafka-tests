package com.avast.kafkatests;

public class GeneratorProducerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new GeneratorProducer(
                configuration.producerConfiguration(),
                configuration.kafkaTopic(),
                configuration.producerInstances(),
                configuration.messagesPerGroup(),
                configuration.producerSlowDown(),
                configuration.shutdownTimeout(),
                new RedisStateDao(configuration.redisServer()));
    }
}
