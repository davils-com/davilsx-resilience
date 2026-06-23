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
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@KoreDsl
public class RateLimiterBuilder internal constructor() : ResilienceComponentBuilder<RateLimiterData>() {

    public var limitForPeriod: Int = 50
    public var limitRefreshPeriod: Duration = 500.nanoseconds
    public var timeoutDuration: Duration = 5.seconds

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

    override fun data(): RateLimiterData {
        val eventData = eventBuilder.produce()
        return RateLimiterData(
            limitForPeriod = limitForPeriod,
            limitRefreshPeriod = limitRefreshPeriod,
            timeoutDuration = timeoutDuration,
            eventData = eventData
        )
    }
}
