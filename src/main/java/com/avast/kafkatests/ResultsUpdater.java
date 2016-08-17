package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computation of results, only a single instance per whole cluster must be executed.
 */
public class ResultsUpdater implements RunnableComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUpdater.class);

    private final ScheduledExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Duration shutdownTimeout;
    private final StateDao stateDao;
    private final int messagesPerGroup;
    private final int checkBeforeFailure;
    private final List<ConsumerType> consumerTypes;

    public ResultsUpdater(Duration shutdownTimeout, Duration updatePeriod, StateDao stateDao, int messagesPerGroup, int checkBeforeFailure,
                          List<ConsumerType> consumerTypes) {
        this.stateDao = stateDao;
        this.shutdownTimeout = shutdownTimeout;
        this.messagesPerGroup = messagesPerGroup;
        this.checkBeforeFailure = checkBeforeFailure;
        this.consumerTypes = consumerTypes;

        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(getClass().getSimpleName() + "-timer-%d")
                .setDaemon(false)
                .build());

        executor.scheduleWithFixedDelay(this, 0, updatePeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        LOGGER.info("Closing instance");
        finish.set(true);

        Utils.shutdownAndWaitTermination(executor, shutdownTimeout, getClass().getSimpleName());
        stateDao.close();
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("====================== Updating state... ======================");
            updateState();
            printState();
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred: {}", e.toString(), e);
        }
    }

    private void updateState() {
        stateDao.listGroupStates(consumerTypes).stream().forEach(this::processGroup);
    }

    private void processGroup(GroupState group) {
        if (group.isComplete(messagesPerGroup)) {
            LOGGER.trace("Group successfully processed: {}", group);
            stateDao.success(group, messagesPerGroup);
        } else {
            if (group.getChecks() > checkBeforeFailure) {
                LOGGER.error("Group processing failed: {}", group);
                stateDao.failure(group, messagesPerGroup);
            } else {
                LOGGER.debug("Not yet: {}", group);
                stateDao.markChecks(group.getKey());
            }
        }
    }

    private void printState() {
        TotalState state = stateDao.totalState(consumerTypes);

        LOGGER.info("State: {} \t\t send requests", state.getSend());
        LOGGER.info("State: {} \t\t send duplications", state.getDuplicationsSend());
        LOGGER.info("State: {} \t\t send bits fail", state.getBitsFailureSend());

        LOGGER.info("State: {} \t\t send confirm success", state.getSendConfirm());
        LOGGER.info("State: {} \t\t send confirm fail", state.getSendFail());
        LOGGER.info("State: {} \t\t send confirm duplications", state.getDuplicationsSendConfirm());
        LOGGER.info("State: {} \t\t send confirm bits fail", state.getBitsFailureConfirm());

        state.getConsume()
                .forEach(c -> LOGGER.info("State: {} \t\t consume {}", c.getCount(), c.getConsumerType()));
        state.getDuplicationsConsume()
                .forEach(c -> LOGGER.info("State: {} \t\t consume {} duplications", c.getCount(), c.getConsumerType()));
        state.getBitsFailureConsume()
                .forEach(c -> LOGGER.info("State: {} \t\t consume {} bits fail", c.getCount(), c.getConsumerType()));

        LOGGER.info("State: {} \t\t consume seeking skip (consumed multiple times due to simulated error)", state.getConsumeSeekingSkip());

        LOGGER.info("State: {} \t\t success bits", state.getBitsSuccess());

        LOGGER.info("State: {} \t\t total not confirmed", state.getSend() - state.getSendConfirm());
        LOGGER.info("State: {} \t\t total not success", state.getSend() - state.getBitsSuccess());

        state.getConsume()
                .forEach(c -> LOGGER.info("State: {} \t\t not consumed {}", state.getSend() - c.getCount(), c.getConsumerType()));
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();
        Utils.closeOnShutdown(new ResultsUpdaterBuilder().newInstance());
        Utils.loopWithNoExit();
    }
}
