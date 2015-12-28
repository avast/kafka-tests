package com.avast.kafkatests;

public class GeneratorProducerBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new GeneratorProducer(
                Configuration.producerConfiguration(),
                Configuration.kafkaTopic(),
                Configuration.producerInstances(),
                Configuration.messagesPerGroup(),
                Configuration.producerSlowDown(),
                Configuration.shutdownTimeout(),
                new RedisStateDao(Configuration.redisServer()));
    }
}
