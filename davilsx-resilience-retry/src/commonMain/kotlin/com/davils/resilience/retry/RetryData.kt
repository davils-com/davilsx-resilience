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
     * The maximum number of attempts allowed, including the initial call.
     *
     * A value of 1 means the operation is executed once without any retry.
     *
     * @since 1.0.0
     */
    val maxAttempts: Int,

    /**
     * If true, the retry will fail after the maximum number of retries.
     * If false, the retry will continue indefinitely until a successful result is obtained.
     *
     * @since 1.0.0
     */
    val failAfterMaxRetries: Boolean,

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
