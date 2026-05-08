package com.davils.resilience.retry

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.retry.predicate.Predicate
import com.davils.resilience.retry.predicate.alwaysRetryOnThrowablePredicate
import com.davils.resilience.retry.strategy.BackoffStrategy
import com.davils.resilience.retry.strategy.constant.constantBackoff

/**
 * A builder class for configuring and creating `Retry` instances.
 *
 * This builder provides a DSL-friendly way to define retry policies, including
 * the maximum number of attempts, backoff strategies, and retry conditions.
 *
 * @since 1.0.0
 */
@KoreDsl
public class RetryBuilder internal constructor() {
    /**
     * The maximum number of attempts to perform, including the initial call.
     *
     * Defaults to 3. Must be at least 1 because the block is always executed
     * once.
     *
     * @since 1.0.0
     */
    public var maxAttempts: Int = 3
        set(value) {
            require(value >= 1) { "maxAttempts must be at least 1" }
            field = value
        }

    /**
     * If true, the retry will fail after the maximum number of retries.
     * If false, the retry will continue indefinitely until a successful result is obtained.
     *
     * Defaults to true.
     * @since 1.0.0
     */
    public var failAfterMaxRetries: Boolean = true

    /**
     * The strategy used to determine the delay between retry attempts.
     *
     * Defaults to a constant backoff of 1000ms.
     *
     * @since 1.0.0
     */
    public var backoffStrategy: BackoffStrategy = constantBackoff()

    /**
     * The predicate that determines whether a retry should be attempted.
     *
     * Defaults to [alwaysRetryOnThrowablePredicate], which retries on any [Throwable].
     *
     * @since 1.0.0
     */
    public var predicate: Predicate = alwaysRetryOnThrowablePredicate()

    /**
     * The behavior applied when a result-based retry exhausts [maxAttempts] while
     * [failAfterMaxRetries] is true.
     *
     * Defaults to [OnResultExhaustion.THROW] so that values previously rejected by the configured
     * [predicate] do not silently leak back to the caller as if they had been accepted. Set this
     * to [OnResultExhaustion.RETURN_LAST] to restore the pre-1.2.0 behavior of returning the last
     * observed value.
     *
     * This setting has no effect on exception-based exhaustion: the last caught [Throwable] is
     * always rethrown.
     *
     * @since 1.2.0
     */
    public var onResultExhaustion: OnResultExhaustion = OnResultExhaustion.THROW

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxAttempts The maximum number of retries. Must be non-negative.
     * @since 1.0.0
     */
    public fun maxAttempts(maxAttempts: Int) {
        this.maxAttempts = maxAttempts
    }

    /**
     * Sets the backoff strategy for calculating delays between attempts.
     *
     * @param strategy The backoff strategy to use.
     * @since 1.0.0
     */
    public fun backoffStrategy(strategy: BackoffStrategy) {
        this.backoffStrategy = strategy
    }

    /**
     * Sets the retry predicate to evaluate failures.
     *
     * @param predicate The predicate to use.
     * @since 1.0.0
     */
    public fun predicate(predicate: Predicate) {
        this.predicate = predicate
    }

    internal fun build(): RetryData {
        return RetryData(
            maxAttempts = maxAttempts,
            backoffStrategy = backoffStrategy,
            predicate = predicate,
            failAfterMaxRetries = failAfterMaxRetries,
            onResultExhaustion = onResultExhaustion
        )
    }
}
