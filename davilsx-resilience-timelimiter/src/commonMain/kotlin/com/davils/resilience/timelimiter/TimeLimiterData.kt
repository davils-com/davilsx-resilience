package com.davils.resilience.timelimiter

import kotlin.time.Duration

@ConsistentCopyVisibility
public data class TimeLimiterData<T> internal constructor(
    val timeout: Duration,
    val cancelOnTimeout: Boolean = true,
    val strategy: TimeoutStrategy = TimeoutStrategy.HARD,
    val fallback: (suspend (Throwable) -> T?)? = null,
)
