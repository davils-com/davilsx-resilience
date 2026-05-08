package com.davils.resilience.retry.strategy

import kotlin.time.Duration

/**
 * Defines a strategy for calculating the delay between retry attempts.
 *
 * Backoff strategies are used to determine how long the system should wait before
 * retrying a failed operation. Common strategies include constant, exponential,
 * and jitter-based delays.
 *
 * @since 1.0.0
 */
public interface BackoffStrategy {
    /**
     * Calculates the delay for the given retry attempt.
     *
     * @param attempt The current retry attempt number (starting from 1).
     * @return The duration to wait before the next attempt.
     * @since 1.0.0
     */
    public fun calculateDelay(attempt: Int): Duration
}
