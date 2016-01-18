package com.avast.kafkatests;

public class GeneratorProducerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new GeneratorProducer(
                configuration.producerConfiguration(),
                configuration.getKafkaTopic(),
                configuration.getProducerInstances(),
                configuration.getMessagesPerGroup(),
                configuration.getProducerSlowDown(),
                configuration.getShutdownTimeout(),
                new RedisStateDao(configuration.getRedisServer()));
    }
}
