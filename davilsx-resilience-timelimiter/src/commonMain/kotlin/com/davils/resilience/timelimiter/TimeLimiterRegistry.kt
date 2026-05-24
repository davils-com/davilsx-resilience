package com.davils.resilience.timelimiter

import com.davils.resilience.common.registry.Registry

public class TimeLimiterRegistry<T> : Registry<TimeLimiterAsync<T>>()