package com.avast.kafkatests.runner;

import com.avast.kafkatests.Configuration;
import com.avast.kafkatests.Kafka09Producer;
import com.avast.kafkatests.RedisStateDao;
import com.avast.kafkatests.RunnableComponent;

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
