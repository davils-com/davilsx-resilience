package com.davils.resilience.timelimiter

import com.davils.resilience.common.event.DisposedEvent
import com.davils.resilience.common.event.ResilienceEvent

public sealed class TimeLimiterEvent : ResilienceEvent() {
    public data class TimeoutExceeded(public val timeoutMs: Long) : TimeLimiterEvent()
    public data object TimeLimiterDisposed : TimeLimiterEvent()
    public data object Disposed : TimeLimiterEvent(), DisposedEvent
}