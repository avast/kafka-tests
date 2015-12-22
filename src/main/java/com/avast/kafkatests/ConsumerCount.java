package com.avast.kafkatests;

/**
 * Consumer type and some count value.
 */
public class ConsumerCount {
    private final ConsumerType consumerType;
    private final int count;

    public ConsumerCount(ConsumerType consumerType, int count) {
        this.consumerType = consumerType;
        this.count = count;
    }

    public ConsumerType getConsumerType() {
        return consumerType;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return consumerType + ", " + count;
    }
}
