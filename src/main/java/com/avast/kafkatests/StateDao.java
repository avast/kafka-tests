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

    void markConsumeAutoCommit(UUID key, int value);

    void close();

    List<GroupState> listGroupStates();

    void success(UUID key, int messagesPerGroup);

    void failure(UUID key, int send, int confirm, int consume, int consumeSeeking);

    void markChecks(UUID key);

    TotalState totalState();

    void markConsumeSeeking(UUID key, int value);

    void markConsumeSeekingSkip(UUID key, int value);
}
