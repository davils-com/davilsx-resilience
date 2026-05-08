package com.davils.resilience.retry.strategy.exponential

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Data class containing the configuration for an exponential backoff strategy.
 *
 * This class is typically instantiated via [ExponentialBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ExponentialBackoffStrategyData internal constructor(
    /**
     * The maximum duration to wait between retry attempts.
     *
     * @since 1.0.0
     */
    public val maxDelay: Duration,

    /**
     * The factor by which the delay increases with each attempt.
     *
     * @since 1.0.0
     */
    public val multiplier: Double,

    /**
     * The initial duration to wait before the first retry attempt.
     *
     * @since 1.0.0
     */
    public val initialDelay: Duration
)
