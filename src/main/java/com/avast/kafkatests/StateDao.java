package com.avast.kafkatests;

import java.util.List;
import java.util.UUID;

/**
 * Confirmations of messages.
 */
public interface StateDao extends AutoCloseable {
    void markSend(UUID key, int value);

    void markSendConfirm(UUID key, int value);

    void markSendFail(UUID key, int value);

    void markConsume(ConsumerType consumerType, UUID key, int value);

    void close();

    List<GroupState> listGroupStates(List<ConsumerType> consumerTypes);

    void success(GroupState group, int messagesPerGroup);

    void failure(GroupState group, int messagesPerGroup);

    void markChecks(UUID key);

    TotalState totalState(List<ConsumerType> consumerTypes);

    void markConsumeSeekingSkip(UUID key, int value);
}
