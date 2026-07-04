/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.resilience.ratelimiter

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a rate limiter with a fixed refresh window.
 *
 * @param limit Maximum permits per [period]. Defaults to 50.
 * @param period Duration of one refresh period. Defaults to 500 milliseconds.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [RateLimiter] instance.
 * @since 1.0.0
 */
public fun fixedRateLimiter(
    limit: Int = 50,
    period: Duration = 500.milliseconds,
    builder: RateLimiterBuilder.() -> Unit = {},
): RateLimiter = rateLimiter {
    limitForPeriod = limit
    limitRefreshPeriod = period
    windowType = RateLimiterWindowType.FIXED
    builder()
}

/**
 * Creates a rate limiter with a sliding time window.
 *
 * @param limit Maximum permits within [period]. Defaults to 50.
 * @param period Rolling window duration. Defaults to 500 milliseconds.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [RateLimiter] instance.
 * @since 1.0.0
 */
public fun slidingWindowRateLimiter(
    limit: Int = 50,
    period: Duration = 500.milliseconds,
    builder: RateLimiterBuilder.() -> Unit = {},
): RateLimiter = rateLimiter {
    limitForPeriod = limit
    limitRefreshPeriod = period
    windowType = RateLimiterWindowType.SLIDING
    builder()
}
