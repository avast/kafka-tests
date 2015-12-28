package com.avast.kafkatests;

public class ResultsUpdaterBuilder implements ComponentBuilder {
    @Override
    public RunnableComponent newInstance() {
        return new ResultsUpdater(
                Configuration.shutdownTimeout(),
                Configuration.updateStatePeriod(),
                new RedisStateDao(Configuration.redisServer()),
                Configuration.messagesPerGroup(),
                Configuration.checksBeforeFailure(),
                Configuration.consumerTypes());
    }
}
