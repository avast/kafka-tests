package com.avast.kafkatests;

/**
 * Total state.
 */
public class TotalState {
    private final int send;
    private final int sendConfirm;
    private final int sendFail;
    private final int consume;
    private final int consumeSeeking;
    private final int consumeSeekingSkip;

    private final int duplicationsSend;
    private final int duplicationsSendConfirm;
    private final int duplicationsConsumeAutoCommit;
    private final int duplicationsConsumeSeeking;

    private final int bitsSuccess;
    private final int bitsFailureSend;
    private final int bitsFailureConfirm;
    private final int bitsFailureConsumeAutoCommit;
    private final int bitsFailureConsumeSeeking;

    public TotalState(int send, int sendConfirm, int sendFail, int consume, int consumeSeeking, int consumeSeekingSkip,
                      int duplicationsSend, int duplicationsSendConfirm, int duplicationsConsumeAutoCommit, int duplicationsConsumeSeeking,
                      int bitsSuccess, int bitsFailureSend, int bitsFailureConfirm, int bitsFailureConsumeAutoCommit, int bitsFailureConsumeSeeking) {
        this.send = send;
        this.sendConfirm = sendConfirm;
        this.sendFail = sendFail;
        this.consume = consume;
        this.consumeSeeking = consumeSeeking;
        this.consumeSeekingSkip = consumeSeekingSkip;

        this.duplicationsSend = duplicationsSend;
        this.duplicationsSendConfirm = duplicationsSendConfirm;
        this.duplicationsConsumeAutoCommit = duplicationsConsumeAutoCommit;
        this.duplicationsConsumeSeeking = duplicationsConsumeSeeking;

        this.bitsSuccess = bitsSuccess;
        this.bitsFailureSend = bitsFailureSend;
        this.bitsFailureConfirm = bitsFailureConfirm;
        this.bitsFailureConsumeAutoCommit = bitsFailureConsumeAutoCommit;
        this.bitsFailureConsumeSeeking = bitsFailureConsumeSeeking;
    }

    public int getSend() {
        return send;
    }

    public int getSendConfirm() {
        return sendConfirm;
    }

    public int getSendFail() {
        return sendFail;
    }

    public int getConsume() {
        return consume;
    }

    public int getConsumeSeeking() {
        return consumeSeeking;
    }

    public int getConsumeSeekingSkip() {
        return consumeSeekingSkip;
    }

    public int getDuplicationsSend() {
        return duplicationsSend;
    }

    public int getDuplicationsSendConfirm() {
        return duplicationsSendConfirm;
    }

    public int getDuplicationsConsumeAutoCommit() {
        return duplicationsConsumeAutoCommit;
    }

    public int getDuplicationsConsumeSeeking() {
        return duplicationsConsumeSeeking;
    }

    public int getBitsSuccess() {
        return bitsSuccess;
    }

    public int getBitsFailureSend() {
        return bitsFailureSend;
    }

    public int getBitsFailureConfirm() {
        return bitsFailureConfirm;
    }

    public int getBitsFailureConsumeAutoCommit() {
        return bitsFailureConsumeAutoCommit;
    }

    public int getBitsFailureConsumeSeeking() {
        return bitsFailureConsumeSeeking;
    }
}
