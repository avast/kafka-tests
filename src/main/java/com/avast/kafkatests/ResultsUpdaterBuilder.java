package com.avast.kafkatests;

public class ResultsUpdaterBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        Configuration configuration = new Configuration();

        return new ResultsUpdater(
                configuration.getShutdownTimeout(),
                configuration.getUpdateStatePeriod(),
                new RedisStateDao(configuration.getRedisServer()),
                configuration.getMessagesPerGroup(),
                configuration.getChecksBeforeFailure(),
                configuration.getConsumerTypes());
    }
}
