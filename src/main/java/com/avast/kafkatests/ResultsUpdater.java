package com.avast.kafkatests;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Computation of results.
 */
public class ResultsUpdater implements AutoCloseable, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultsUpdater.class);

    private final ScheduledExecutorService executor;
    private final AtomicBoolean finish = new AtomicBoolean(false);
    private final Duration shutdownTimeout;
    private final StateDao stateDao;
    private final int messagesPerGroup;
    private final int checkBeforeFailure;

    public ResultsUpdater(Duration shutdownTimeout, Duration updatePeriod, StateDao stateDao, int messagesPerGroup, int checkBeforeFailure) {
        this.stateDao = stateDao;
        this.shutdownTimeout = shutdownTimeout;
        this.messagesPerGroup = messagesPerGroup;
        this.checkBeforeFailure = checkBeforeFailure;

        this.executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(getClass().getSimpleName() + "-timer-%d")
                .setDaemon(false)
                .build());

        executor.scheduleWithFixedDelay(this, 0, updatePeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
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
            LOGGER.error("Unexpected exception occurred: {}", e, e);
        }
    }

    private void updateState() {
        stateDao.listGroupStates().stream().forEach(this::processGroup);
    }

    private void processGroup(GroupState group) {
        if (group.getSend() == messagesPerGroup && group.getConfirm() == messagesPerGroup
                && group.getConsumeAutoCommit() == messagesPerGroup && group.getConsumeSeeking() == messagesPerGroup) {
            LOGGER.trace("Group successfully processed: {}", group);
            stateDao.success(group.getKey(), messagesPerGroup);
        } else {
            if (group.getChecks() > checkBeforeFailure) {
                LOGGER.error("Group processing failed: {}", group);
                stateDao.failure(group.getKey(), messagesPerGroup - group.getSend(), messagesPerGroup - group.getConfirm(),
                        messagesPerGroup - group.getConsumeAutoCommit(), messagesPerGroup - group.getConsumeSeeking());
            } else {
                LOGGER.debug("Not yet: {}", group);
                stateDao.markChecks(group.getKey());
            }
        }
    }

    private void printState() {
        TotalState state = stateDao.totalState();

        LOGGER.info("State: {} \t\t send requests", state.getSend());
        LOGGER.info("State: {} \t\t send duplications", state.getDuplicationsSend());
        LOGGER.info("State: {} \t\t send bits fail", state.getBitsFailureSend());

        LOGGER.info("State: {} \t\t send confirm success", state.getSendConfirm());
        LOGGER.info("State: {} \t\t send confirm fail", state.getSendFail());
        LOGGER.info("State: {} \t\t send confirm duplications", state.getDuplicationsSendConfirm());
        LOGGER.info("State: {} \t\t send confirm bits fail", state.getBitsFailureConfirm());

        LOGGER.info("State: {} \t\t consume auto commit", state.getConsume());
        LOGGER.info("State: {} \t\t consume auto commit duplications", state.getDuplicationsConsumeAutoCommit());
        LOGGER.info("State: {} \t\t consume auto commit bits fail", state.getBitsFailureConsumeAutoCommit());

        LOGGER.info("State: {} \t\t consume seeking", state.getConsumeSeeking());
        LOGGER.info("State: {} \t\t consume seeking skip (consumed multiple times due to simulated error)", state.getConsumeSeekingSkip());
        LOGGER.info("State: {} \t\t consume seeking duplications", state.getDuplicationsConsumeSeeking());
        LOGGER.info("State: {} \t\t consume seeking bits fail", state.getBitsFailureConsumeSeeking());

        LOGGER.info("State: {} \t\t success bits", state.getBitsSuccess());

        LOGGER.info("State: {} \t\t total not confirmed", state.getSend() - state.getSendConfirm());
        LOGGER.info("State: {} \t\t total not consumed auto commit", state.getSend() - state.getConsume());
        LOGGER.info("State: {} \t\t total not success", state.getSend() - state.getBitsSuccess());
        LOGGER.info("State: {} \t\t total not consumed seeking", state.getSend() - state.getConsumeSeeking());
    }

    public static void main(String[] args) {
        Utils.logAllUnhandledExceptions();

        ResultsUpdater instance = new ResultsUpdater(
                Configuration.shutdownTimeout(),
                Configuration.updateStatePeriod(),
                new RedisStateDao(Configuration.redisServer()),
                Configuration.messagesPerGroup(),
                Configuration.checksBeforeFailure());

        Utils.closeOnShutdown(instance);
        Utils.loopWithNoExit();
    }
}
