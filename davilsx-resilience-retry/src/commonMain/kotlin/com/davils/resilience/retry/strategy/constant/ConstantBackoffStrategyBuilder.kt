package com.davils.resilience.retry.strategy.constant

import com.davils.kore.annotation.KoreDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A builder class for creating instances of [ConstantBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure the delay for a constant
 * backoff strategy. It ensures that the provided delay is non-negative.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ConstantBackoffStrategyBuilder internal constructor(){
    /**
     * The fixed delay duration between retry attempts.
     *
     * Defaults to 1000 milliseconds. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var delay: Duration = 1000.milliseconds
        set(value) {
            require(!value.isNegative()) { "delay must be non-negative" }
            field = value
        }

    /**
     * Sets the fixed delay duration between retry attempts.
     *
     * @param delay The duration to wait between attempts.
     * @since 1.0.0
     */
    public fun delay(delay: Duration) {
        this.delay = delay
    }

    /**
     * Sets the fixed delay duration between retry attempts in milliseconds.
     *
     * @param delayMillis The duration in milliseconds to wait between attempts.
     * @since 1.0.0
     */
    public fun delay(delayMillis: Long) {
        this.delay = delayMillis.milliseconds
    }

    internal fun build() = ConstantBackoffStrategyData(delay)
}