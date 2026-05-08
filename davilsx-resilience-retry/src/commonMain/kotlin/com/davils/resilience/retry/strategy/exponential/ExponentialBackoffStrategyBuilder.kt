package com.davils.resilience.retry.strategy.exponential

import com.davils.kore.annotation.KoreDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A builder class for creating instances of [ExponentialBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure parameters for an
 * exponential backoff strategy, such as initial delay, multiplier, and maximum delay.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ExponentialBackoffStrategyBuilder internal constructor() {
    /**
     * The maximum duration to wait between retry attempts.
     *
     * Defaults to 60,000 milliseconds (1 minute). Must be non-negative.
     *
     * @since 1.0.0
     */
    public var maxDelay: Duration = 60000.milliseconds
        set(value) {
            require(!value.isNegative()) { "maxDelay must be non-negative" }
            field = value
        }

    /**
     * The factor by which the delay increases with each attempt.
     *
     * Defaults to 2.0. Must be greater than 0.
     *
     * @since 1.0.0
     */
    public var multiplier: Double = 2.0
        set(value) {
            require(value > 0) { "multiplier must be greater than 0" }
            field = value
        }

    /**
     * The initial duration to wait before the first retry attempt.
     *
     * Defaults to 1000 milliseconds. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var initialDelay: Duration = 1000.milliseconds
        set(value) {
            require(!value.isNegative()) { "initialDelay must be non-negative" }
            field = value
        }

    /**
     * Sets the maximum duration to wait between retry attempts.
     *
     * @param maxDelay The maximum duration.
     * @since 1.0.0
     */
    public fun maxDelay(maxDelay: Duration) {
        this.maxDelay = maxDelay
    }

    /**
     * Sets the maximum duration to wait between retry attempts in milliseconds.
     *
     * @param maxDelayMillis The maximum duration in milliseconds.
     * @since 1.0.0
     */
    public fun maxDelay(maxDelayMillis: Long) {
        this.maxDelay = maxDelayMillis.milliseconds
    }

    /**
     * Sets the factor by which the delay increases with each attempt.
     *
     * @param multiplier The growth factor.
     * @since 1.0.0
     */
    public fun multiplier(multiplier: Double) {
        this.multiplier = multiplier
    }

    /**
     * Sets the initial duration to wait before the first retry attempt.
     *
     * @param initialDelay The initial duration.
     * @since 1.0.0
     */
    public fun initialDelay(initialDelay: Duration) {
        this.initialDelay = initialDelay
    }

    /**
     * Sets the initial duration to wait before the first retry attempt in milliseconds.
     *
     * @param initialDelayMillis The initial duration in milliseconds.
     * @since 1.0.0
     */
    public fun initialDelay(initialDelayMillis: Long) {
        this.initialDelay = initialDelayMillis.milliseconds
    }

    internal fun build() = ExponentialBackoffStrategyData(
        maxDelay = maxDelay,
        multiplier = multiplier,
        initialDelay = initialDelay
    )
}