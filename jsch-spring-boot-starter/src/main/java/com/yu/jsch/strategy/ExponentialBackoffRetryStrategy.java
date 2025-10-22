package com.yu.jsch.strategy;

import java.util.concurrent.TimeUnit;

/**
 * Exponential backoff retry strategy.
 */
public class ExponentialBackoffRetryStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long baseDelayMillis;
    private final double multiplier;
    private final Long maxDelayMillis;

    private ExponentialBackoffRetryStrategy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.baseDelayMillis = builder.baseDelayMillis;
        this.multiplier = builder.multiplier;
        this.maxDelayMillis = builder.maxDelayMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean shouldRetry(int attempt, Throwable lastError) {
        return attempt <= maxAttempts;
    }

    @Override
    public long getDelayMillis(int attempt) {
        if (attempt <= 0) return 0L;
        double factor = Math.pow(multiplier, Math.max(0, attempt - 1));
        long delay = (long) Math.floor(baseDelayMillis * factor);
        if (maxDelayMillis != null && delay > maxDelayMillis) {
            return maxDelayMillis;
        }
        return delay;
    }

    public static final class Builder {
        private int maxAttempts = 3;
        private long baseDelayMillis = TimeUnit.MILLISECONDS.toMillis(200);
        private double multiplier = 2.0d;
        private Long maxDelayMillis = null;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder baseDelayMillis(long baseDelayMillis) {
            this.baseDelayMillis = baseDelayMillis;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxDelayMillis(Long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
            return this;
        }

        public ExponentialBackoffRetryStrategy build() {
            return new ExponentialBackoffRetryStrategy(this);
        }
    }
}
