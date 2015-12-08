package com.avast.kafkatests;

import java.util.UUID;

/**
 * State of a group in storage.
 */
public class GroupState {
    private final UUID key;
    private final int send;
    private final int confirm;
    private final int consumeAutoCommit;
    private final int consumeSeeking;
    private final int checks;

    public GroupState(UUID key, int send, int confirm, int consumeAutoCommit, int consumeSeeking, int checks) {
        this.key = key;
        this.send = send;
        this.confirm = confirm;
        this.consumeAutoCommit = consumeAutoCommit;
        this.consumeSeeking = consumeSeeking;
        this.checks = checks;
    }

    public UUID getKey() {
        return key;
    }

    public int getSend() {
        return send;
    }

    public int getConfirm() {
        return confirm;
    }

    public int getConsumeAutoCommit() {
        return consumeAutoCommit;
    }

    public int getConsumeSeeking() {
        return consumeSeeking;
    }

    public int getChecks() {
        return checks;
    }

    @Override
    public String toString() {
        return key + ", " + send + " send, " + confirm + " confirm, " + consumeAutoCommit + " consume auto commit, "
                + consumeSeeking + " consume seeking, " + checks + " checks";
    }
}
