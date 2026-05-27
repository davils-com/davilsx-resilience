package com.davils.resilience.timelimiter

import com.davils.kore.pattern.reactive.event.EventMarker

public sealed class TimeLimiterEvent : EventMarker() {
    public data class TimeoutExceeded(public val timeoutMs: Long) : TimeLimiterEvent()
    public data object TimeLimiterDisposed : TimeLimiterEvent()
}
