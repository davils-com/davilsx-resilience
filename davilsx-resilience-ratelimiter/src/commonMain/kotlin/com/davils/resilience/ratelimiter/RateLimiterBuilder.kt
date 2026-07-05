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

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * DSL builder for configuring a [RateLimiter].
 *
 * @since 1.0.0
 */
@KoreDsl
public class RateLimiterBuilder internal constructor() : ResilienceComponentBuilder<RateLimiterData>() {

    /** Maximum number of permits available per [limitRefreshPeriod]. Defaults to 50. */
    public var limitForPeriod: Int = 50

    /** Duration after which the permit count is refreshed. Defaults to 500 milliseconds. */
    public var limitRefreshPeriod: Duration = 500.milliseconds

    /** Maximum time to wait for a permit before rejecting. Defaults to 5 seconds. */
    public var timeoutDuration: Duration = 5.seconds

    /** Strategy applied when a permit is not immediately available. Defaults to [RateLimiterStrategy.WAIT]. */
    public var strategy: RateLimiterStrategy = RateLimiterStrategy.WAIT

    /** Window algorithm used to enforce the rate limit. Defaults to [RateLimiterWindowType.FIXED]. */
    public var windowType: RateLimiterWindowType = RateLimiterWindowType.FIXED

    public fun limitForPeriod(limitForPeriod: Int) {
        this.limitForPeriod = limitForPeriod
    }

    public fun limitRefreshPeriod(limitRefreshPeriod: Duration) {
        this.limitRefreshPeriod = limitRefreshPeriod
    }

    public fun timeoutDuration(timeoutDuration: Duration) {
        this.timeoutDuration = timeoutDuration
    }

    public fun timeout(timeout: Duration) {
        this.timeoutDuration = timeout
    }

    public fun strategy(strategy: RateLimiterStrategy) {
        this.strategy = strategy
    }

    public fun windowType(windowType: RateLimiterWindowType) {
        this.windowType = windowType
    }

    override fun data(): RateLimiterData {
        val eventData = eventBuilder.produce()
        return RateLimiterData(
            limitForPeriod = limitForPeriod,
            limitRefreshPeriod = limitRefreshPeriod,
            timeoutDuration = timeoutDuration,
            strategy = strategy,
            windowType = windowType,
            eventData = eventData,
        )
    }
}
