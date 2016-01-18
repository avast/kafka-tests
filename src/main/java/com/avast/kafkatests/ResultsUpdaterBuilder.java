package com.avast.kafkatests;

public class ResultsUpdaterBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new ResultsUpdater(
                configuration.shutdownTimeout(),
                configuration.updateStatePeriod(),
                new RedisStateDao(configuration.redisServer()),
                configuration.messagesPerGroup(),
                configuration.checksBeforeFailure(),
                configuration.consumerTypes());
    }
}
