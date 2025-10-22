package com.yu.jsch.strategy;

/**
 * Strategy for retrying failed operations.
 * Attempt numbers are 1-based: attempt=1 is the first retry after an initial failure.
 */
public interface RetryStrategy {
    /**
     * Whether another retry should be attempted.
     *
     * @param attempt   1-based attempt number for the next retry
     * @param lastError the last error encountered
     * @return true if a retry should be performed
     */
    boolean shouldRetry(int attempt, Throwable lastError);

    /**
     * Delay in milliseconds before the specified attempt number is executed.
     *
     * @param attempt 1-based attempt number
     * @return delay in milliseconds
     */
    long getDelayMillis(int attempt);
}
