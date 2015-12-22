package com.avast.kafkatests;

import java.util.List;

/**
 * Total state.
 */
public class TotalState {
    private final int send;
    private final int sendConfirm;
    private final int sendFail;
    private final List<ConsumerCount> consume;
    private final int consumeSeekingSkip;

    private final int duplicationsSend;
    private final int duplicationsSendConfirm;
    private final List<ConsumerCount> duplicationsConsume;

    private final int bitsSuccess;
    private final int bitsFailureSend;
    private final int bitsFailureConfirm;
    private final List<ConsumerCount> bitsFailureConsume;

    public TotalState(int send, int sendConfirm, int sendFail, List<ConsumerCount> consume, int consumeSeekingSkip,
                      int duplicationsSend, int duplicationsSendConfirm, List<ConsumerCount> duplicationsConsume,
                      int bitsSuccess, int bitsFailureSend, int bitsFailureConfirm, List<ConsumerCount> bitsFailureConsume) {
        this.send = send;
        this.sendConfirm = sendConfirm;
        this.sendFail = sendFail;
        this.consume = consume;
        this.consumeSeekingSkip = consumeSeekingSkip;

        this.duplicationsSend = duplicationsSend;
        this.duplicationsSendConfirm = duplicationsSendConfirm;
        this.duplicationsConsume = duplicationsConsume;

        this.bitsSuccess = bitsSuccess;
        this.bitsFailureSend = bitsFailureSend;
        this.bitsFailureConfirm = bitsFailureConfirm;
        this.bitsFailureConsume = bitsFailureConsume;
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

    public List<ConsumerCount> getConsume() {
        return consume;
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

    public List<ConsumerCount> getDuplicationsConsume() {
        return duplicationsConsume;
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

    public List<ConsumerCount> getBitsFailureConsume() {
        return bitsFailureConsume;
    }
}
