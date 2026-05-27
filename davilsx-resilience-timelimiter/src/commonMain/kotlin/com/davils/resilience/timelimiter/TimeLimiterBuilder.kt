package com.davils.resilience.timelimiter

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration


@KoreDsl
public class TimeLimiterBuilder internal constructor() : ResilienceComponentBuilder<TimeLimiterData>() {
    public var timeout: Duration = Duration.ZERO
    public var cancelOnTimeout: Boolean = true
    public var strategy: TimeoutStrategy = TimeoutStrategy.HARD
    public var fallback: (suspend (Throwable) -> Any?)? = null

    public fun timeout(timeout: Duration) {
        this.timeout = timeout
    }

    public fun cancelOnTimeout(cancel: Boolean) {
        this.cancelOnTimeout = cancel
    }

    public fun strategy(strategy: TimeoutStrategy) {
        this.strategy = strategy
    }

    public fun <T> fallback(fallback: suspend (Throwable) -> T?) {
        this.fallback = fallback
    }

    override fun data(): TimeLimiterData {
        val eventData = eventBuilder.produce()
        return TimeLimiterData(
            timeout = timeout,
            cancelOnTimeout = cancelOnTimeout,
            strategy = strategy,
            fallback = fallback,
            eventData = eventData
        )
    }
}
