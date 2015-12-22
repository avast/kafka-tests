package com.avast.kafkatests;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * State of a group in storage.
 */
public class GroupState {
    private final UUID key;
    private final int send;
    private final int confirm;
    private final List<ConsumerCount> consumerCounts;
    private final int checks;

    public GroupState(UUID key, int send, int confirm, List<ConsumerCount> consumerCounts, int checks) {
        this.key = key;
        this.send = send;
        this.confirm = confirm;
        this.consumerCounts = consumerCounts;
        this.checks = checks;
    }

    public UUID getKey() {
        return key;
    }

    public boolean isComplete(int messagesPerGroup) {
        return send == messagesPerGroup
                && confirm == messagesPerGroup
                && consumerCounts.stream().allMatch(c -> c.getCount() == messagesPerGroup);
    }

    public int getSend() {
        return send;
    }

    public int getConfirm() {
        return confirm;
    }

    public List<ConsumerCount> getConsumerCounts() {
        return consumerCounts;
    }

    public int getChecks() {
        return checks;
    }

    @Override
    public String toString() {
        String consumersStat = consumerCounts.stream()
                .map(e -> e.getCount() + " consume " + e.getConsumerType())
                .collect(Collectors.joining(", "));

        return key + ", " + send + " send, " + confirm + " confirm, " + consumersStat + ", " + checks + " checks";
    }
}
