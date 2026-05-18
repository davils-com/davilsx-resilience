package com.davils.resilience.timelimiter

/**
 * Shared, thread-safe helper for time limiter behavior.
 *
 * The provider exposes the immutable [TimeLimiterData] and small helpers that are
 * pure and free of suspension so they can be used by different executors if needed.
 */
public interface TimeLimiterProvider<T> {
    public val data: TimeLimiterData<T>

    public fun hasFallback(): Boolean = data.fallback != null
}

