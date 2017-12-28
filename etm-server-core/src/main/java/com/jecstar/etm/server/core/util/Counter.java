package com.jecstar.etm.server.core.util;

public class Counter {

    private long value;

    private int additions;

    /**
     * Add a value to the total.
     * @param value The value to add.
     *
     * @return This instance.
     */
    public Counter add(long value) {
        this.value += value;
        this.additions++;
        return this;
    }

    /**
     * Resets the counter to zero.
     *
     * @return This instance.
     */
    public Counter reset() {
        this.value = 0;
        this.additions = 0;
        return this;
    }

    /**
     * Gives the average of the added values.
     *
     * @return The average of the added values.
     */
    public long getAverage() {
        if (this.additions == 0) {
            return 0;
        }
        return this.value / this.additions;
    }
}
