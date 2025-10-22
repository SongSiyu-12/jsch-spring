package com.yu.jsch.strategy;

/**
 * No-retry strategy: never retries and returns zero delay.
 */
public class NoRetryStrategy implements RetryStrategy {
    @Override
    public boolean shouldRetry(int attempt, Throwable lastError) {
        return false;
    }

    @Override
    public long getDelayMillis(int attempt) {
        return 0L;
    }
}
