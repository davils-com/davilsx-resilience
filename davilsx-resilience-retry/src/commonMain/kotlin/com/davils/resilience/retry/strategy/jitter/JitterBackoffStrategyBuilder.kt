package com.davils.resilience.retry.strategy.jitter

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.time.Duration

/**
 * A builder class for creating instances of [JitterBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure the jitter algorithm,
 * factor and cap for a [JitterBackoffStrategy].
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
     * Only used when [mode] is [JitterMode.PROPORTIONAL]. Ignored for other modes.
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
     * The jitter algorithm to apply.
     *
     * Defaults to [JitterMode.PROPORTIONAL].
     *
     * @since 1.0.0
     */
    public var mode: JitterMode = JitterMode.PROPORTIONAL

    /**
     * The upper bound applied to the jittered delay.
     *
     * Particularly relevant for [JitterMode.DECORRELATED], which can otherwise grow
     * without bound. Must be strictly positive.
     *
     * Defaults to [Duration.INFINITE], meaning no cap is applied.
     *
     * @since 1.0.0
     */
    public var cap: Duration = Duration.INFINITE
        set(value) {
            require(value > Duration.ZERO) { "cap must be strictly positive" }
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

    /**
     * Sets the jitter algorithm to apply.
     *
     * @param mode The jitter algorithm to use.
     * @since 1.0.0
     */
    public fun mode(mode: JitterMode) {
        this.mode = mode
    }

    /**
     * Sets the upper bound applied to the jittered delay.
     *
     * @param cap The maximum allowed delay. Must be strictly positive.
     * @since 1.0.0
     */
    public fun cap(cap: Duration) {
        this.cap = cap
    }

    internal fun build(): JitterBackoffStrategyData {
        return JitterBackoffStrategyData(
            backoffStrategy = backoffStrategy,
            factor = factor,
            mode = mode,
            cap = cap
        )
    }
}
