package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.BackoffStrategy

/**
 * Data class containing the configuration for a jitter backoff strategy.
 *
 * This class is typically instantiated via [JitterBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class JitterBackoffStrategyData internal constructor(
    /**
     * The base backoff strategy to which jitter is applied.
     *
     * @since 1.0.0
     */
    val backoffStrategy: BackoffStrategy,
    /**
     * The jitter factor to apply.
     *
     * @since 1.0.0
     */
    val factor: Double,
)
