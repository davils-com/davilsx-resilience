package com.davils.resilience.timelimiter

import com.davils.kore.annotation.KoreDsl
import kotlin.time.Duration


@KoreDsl
public class TimeLimiterBuilder<T> internal constructor() {
    public var timeout: Duration = Duration.ZERO
    public var cancelOnTimeout: Boolean = true
    public var strategy: TimeoutStrategy = TimeoutStrategy.HARD
    public var fallback: (suspend (Throwable) -> T?)? = null

    public fun timeout(timeout: Duration) {
        require(!timeout.isNegative()) { "timeout must be non-negative" }
        this.timeout = timeout
    }

    public fun cancelOnTimeout(cancel: Boolean) {
        this.cancelOnTimeout = cancel
    }

    public fun strategy(strategy: TimeoutStrategy) {
        this.strategy = strategy
    }

    public fun fallback(fallback: suspend (Throwable) -> T?) {
        this.fallback = fallback
    }

    internal fun build(): TimeLimiterData<T> = TimeLimiterData(
        timeout = timeout,
        cancelOnTimeout = cancelOnTimeout,
        strategy = strategy,
        fallback = fallback,
    )
}
