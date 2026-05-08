package com.davils.resilience.retry

import kotlin.time.Duration

/**
 * Provides shared, thread-safe retry logic used by both synchronous and asynchronous retry implementations.
 *
 * Implementations expose the underlying [RetryData] configuration that drives the retry behavior.
 * The helper functions defined in this interface contain the pure retry decision logic and are
 * intentionally free of any I/O, blocking, or suspending operations, so they can be safely reused
 * by both blocking and coroutine-based executors.
 *
 * All helpers are stateless with respect to the provider instance: each invocation of an `execute`
 * function on a concrete [Retry] or [RetryAsync] runs on its own local state, which guarantees
 * thread safety even when the same provider instance is shared between threads.
 *
 * @since 1.0.0
 */
public interface RetryProvider {
    /**
     * The immutable configuration that defines the retry behavior of this provider.
     *
     * @since 1.0.0
     */
    public val data: RetryData

    /**
     * Determines whether another retry attempt should be performed for the given failure.
     *
     * The decision is based on the configured [RetryData.predicate], the configured
     * [RetryData.maxRetries] and the [RetryData.failAfterMaxRetries] flag.
     *
     * @param attempt The number of attempts already performed, starting from 1 for the initial call.
     * @param throwable The exception thrown by the last attempt. Must not be null.
     * @return true if another attempt should be performed, false otherwise.
     * @since 1.0.0
     */
    public fun shouldRetry(attempt: Int, throwable: Throwable): Boolean {
        if (!data.predicate.shouldRetry(throwable)) return false
        if (attempt > data.maxRetries && data.failAfterMaxRetries) return false
        return true
    }

    /**
     * Calculates the delay that should be awaited before performing the given retry attempt.
     *
     * Negative durations returned by a custom [com.davils.resilience.retry.strategy.BackoffStrategy]
     * are normalized to [Duration.ZERO] to guarantee safe usage by both blocking and suspending
     * delay primitives.
     *
     * @param nextAttempt The retry attempt number that is about to be executed, starting from 1.
     * @return The non-negative duration to wait before performing the next attempt.
     * @since 1.0.0
     */
    public fun nextDelay(nextAttempt: Int): Duration {
        val delay = data.backoffStrategy.calculateDelay(nextAttempt)
        return if (delay < Duration.ZERO) Duration.ZERO else delay
    }
}

