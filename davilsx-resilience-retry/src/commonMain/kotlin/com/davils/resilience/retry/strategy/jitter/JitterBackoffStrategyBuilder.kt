package com.davils.resilience.retry.strategy.jitter

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.retry.strategy.BackoffStrategy

/**
 * A builder class for creating instances of [JitterBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure the jitter factor for
 * a [JitterBackoffStrategy].
 *
 * @since 1.0.0
 */
@KoreDsl
public class JitterBackoffStrategyBuilder internal constructor(private val backoffStrategy: BackoffStrategy) {
    /**
     * The jitter factor to apply to the base backoff delay.
     *
     * Must be between 0.0 (exclusive) and 1.0 (inclusive). For example, a factor
     * of 0.5 means the delay will vary between 50% and 150% of the base delay.
     *
     * Defaults to 0.5.
     *
     * @since 1.0.0
     */
    public var factor: Double = 0.5
        set(value) {
            require(value > 0.0 && value <= 1.0) { "factor must be between 0.0 (exclusive) and 1.0" }
            field = value
        }

    /**
     * Sets the jitter factor to apply.
     *
     * @param factor The jitter factor.
     * @since 1.0.0
     */
    public fun factor(factor: Double) {
        this.factor = factor
    }

    internal fun build(): JitterBackoffStrategyData {
        return JitterBackoffStrategyData(
            backoffStrategy = backoffStrategy,
            factor = factor
        )
    }
}
