package com.davils.resilience.retry.strategy.constant

import kotlin.time.Duration

/**
 * Data class containing the configuration for a constant backoff strategy.
 *
 * This class is typically instantiated via [ConstantBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ConstantBackoffStrategyData internal constructor(
    /**
     * The fixed delay duration between retry attempts.
     *
     * @since 1.0.0
     */
    val delay: Duration
)
