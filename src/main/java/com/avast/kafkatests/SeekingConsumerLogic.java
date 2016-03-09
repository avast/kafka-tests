package com.avast.kafkatests;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Storage for seeking consumer.
 */
public class SeekingConsumerLogic implements ConsumerRebalanceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeekingConsumerLogic.class);

    private final Random random = new Random();
    private final Consumer<String, Integer> consumer;
    private final StateDao stateDao;
    private final int messagesToChangeState;
    private final int percentFailureProbability;

    private State state = State.SUCCESS;
    private int messages = 0;

    public SeekingConsumerLogic(Consumer<String, Integer> consumer, StateDao stateDao, int messagesToChangeState, int percentFailureProbability) {
        this.consumer = consumer;
        this.stateDao = stateDao;
        this.messagesToChangeState = messagesToChangeState;
        this.percentFailureProbability = percentFailureProbability;
    }

    public void processMessages(ConsumerRecords<String, Integer> records) {
        for (ConsumerRecord<String, Integer> record : records) {
            processMessage(record);
        }

        messages += records.count();

        if (messages > messagesToChangeState || records.isEmpty()) {
            messages = 0;
            optionallyCommitAllOffsets();
            optionallySeekToTheLastCommittedOffsets();
            generateNewState();
        }
    }

    private void processMessage(ConsumerRecord<String, Integer> record) {
        switch (state) {
            case SUCCESS:
                LOGGER.trace("Message consumed: {}, {}, {}-{}/{}", record.key(), record.value(), record.topic(), record.partition(), record.offset());
                stateDao.markConsume(ConsumerType.seeking, UUID.fromString(record.key()), record.value());
                break;

            case FAILURE:
                LOGGER.trace("Message skip: {}, {}, {}-{}/{}", record.key(), record.value(), record.topic(), record.partition(), record.offset());
                stateDao.markConsumeSeekingSkip(UUID.fromString(record.key()), record.value());
                break;

            default:
                throw new IllegalStateException("Unexpected state: " + state);
        }
    }

    private void generateNewState() {
        State previousState = state;

        // Do not allow multiple errors in a row to ensure quick messages flow, success should follow after each error
        state = State.SUCCESS;

        if (previousState == State.SUCCESS) {
            if (random.nextInt(100) < percentFailureProbability) {
                state = State.FAILURE;
            }
        }

        if (state != previousState) {
            LOGGER.info("State changed: {} to {}", previousState, state);
        }
    }

    public void optionallyCommitAllOffsets() {
        if (state != State.SUCCESS) {
            return;
        }

        LOGGER.debug("Committing all offsets...");

        try {
            consumer.commitSync();
        } catch (CommitFailedException e) {
            LOGGER.error("Commit of offsets failed: {}", e, e);
        }
    }

    private void optionallySeekToTheLastCommittedOffsets() {
        if (state != State.FAILURE) {
            return;
        }

        LOGGER.debug("Seeking to the last committed offsets...");
        Set<TopicPartition> assignment = consumer.assignment();

        assignment.stream().forEach(tp -> {
            OffsetAndMetadata committed = consumer.committed(tp);
            consumer.seek(tp, committed.offset());
        });
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        LOGGER.info("Rebalance callback, revoked: {}", partitions);
        optionallyCommitAllOffsets();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        LOGGER.info("Rebalance callback, assigned: {}", partitions);
        // TODO: Seek to the last committed offsets should be automatic
    }

    private enum State {
        SUCCESS, FAILURE
    }
}
