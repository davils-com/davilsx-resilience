package com.davils.resilience.retry

import com.davils.resilience.retry.predicate.Predicate
import com.davils.resilience.retry.strategy.BackoffStrategy

/**
 * Data class representing the configuration for a retry operation.
 *
 * This class holds all parameters necessary to execute a retry logic, including
 * the maximum number of attempts, the backoff strategy, and the retry predicate.
 *
 * @since 1.0.0
 */
public data class RetryData(
    /**
     * The maximum number of retry attempts allowed.
     *
     * A value of 0 means no retries will be attempted (only the initial call).
     *
     * @since 1.0.0
     */
    val maxRetries: Int,

    /**
     * The strategy used to calculate the delay between retry attempts.
     *
     * @since 1.0.0
     */
    val backoffStrategy: BackoffStrategy,

    /**
     * The condition that determines whether a retry should be attempted based on the failure.
     *
     * @since 1.0.0
     */
    val predicate: Predicate
)
