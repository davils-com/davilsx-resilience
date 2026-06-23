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

import com.davils.resilience.common.ResilienceComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeSource


public class RateLimiter internal constructor(
    public override val data: RateLimiterData
) : ResilienceComponent<RateLimiterData, RateLimiterEvent>() {

    override val disposeEvent: RateLimiterEvent
        get() = RateLimiterEvent.RateLimiterDisposed

    private val startTime = TimeSource.Monotonic.markNow()

    public suspend fun acquireSlot(permits: Int = 1): Boolean {
        checkDisposal()
    }

    public suspend fun reserveSlot(permits: Int = 1): Duration {
        checkDisposal()
        return reserveSlotsInternal(permits)
    }

    private suspend fun reserveSlotsInternal(permits: Int): Duration {
        val now = TimeSource.Monotonic.markNow()
        val currentTime = now - startTime

        return mutex.withLock {}
    }

    public suspend fun getAvailableSlots(): Int {
        val now = TimeSource.Monotonic.markNow()
        val currentTime = now - startTime

        return mutex.withLock {}
    }

    private suspend fun checkDisposal()  {
        if (isDisposed()) {
            throw CancellationException("RateLimiter instance is disposed")
        }
    }


}

public fun rateLimiter(builder: RateLimiterBuilder.() -> Unit): RateLimiter {
    val rateLimiterBuilder = RateLimiterBuilder()
    rateLimiterBuilder.builder()
    val data = rateLimiterBuilder.produce()
    return RateLimiter(data)
}
