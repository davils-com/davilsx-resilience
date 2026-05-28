package com.davils.resilience.timelimiter

import com.davils.resilience.common.registry.ResilienceRegistry

public object TimeLimiterRegistry : ResilienceRegistry<TimeLimiterEvent, TimeLimiterData, TimeLimiterBuilder, TimeLimiter>() {
    override fun createBuilder(): TimeLimiterBuilder = TimeLimiterBuilder()

    override fun createComponent(data: TimeLimiterData): TimeLimiter = TimeLimiter(data)
}
