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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource


public class RateLimiter internal constructor(
    public override val data: RateLimiterData
) : ResilienceComponent<RateLimiterData, RateLimiterEvent>() {

    override val disposeEvent: RateLimiterEvent
        get() = RateLimiterEvent.RateLimiterDisposed

    private val startTime = TimeSource.Monotonic.markNow()
    // CycleIndex of the last state update
    private var lastStatusCycle: Long = 0L
    // Remaining permits in the cycle
    private var availableSlots: Int = data.limitForPeriod

    public suspend fun <T> execute(block: suspend () -> T): T = execute(1, block)
    public suspend fun <T> execute(permits: Int, block: suspend () -> T): T {
        if (acquireSlot(permits)) {
            return block()
        } else {
            throw RequestNotPermittedException()
        }
    }

    public suspend fun acquireSlot(permits: Int = 1): Boolean {
        // Waiting for a permit acquisition
        checkDisposal()
        val durationToWait = reserveSlotInternal(permits)
        if (durationToWait <= data.timeoutDuration) {
            if (durationToWait > Duration.ZERO) {
                delay(durationToWait)
            }
            eventBus.push(RateLimiterEvent.SuccessfulAcquire(permits))
            return true
        } else {
            eventBus.push(RateLimiterEvent.FailedAcquire(permits, data.timeoutDuration))
            return false
        }
    }

    public suspend fun reserveSlot(permits: Int = 1): Duration {
        checkDisposal()
        return reserveSlotInternal(permits)
    }

    private suspend fun reserveSlotInternal(permits: Int): Duration {

        // Single request do not exceed total capacity of one refresh period
        require(permits <= data.limitForPeriod) {
            "Requested permits ($permits) cannot be greater than limitForPeriod (${data.limitForPeriod})"
        }

        val now = TimeSource.Monotonic.markNow()
        val elapsedTime = now - startTime

        return mutex.withLock {
            // Calculation of current cycle
            val currentCycle = elapsedTime.inWholeNanoseconds / data.limitRefreshPeriod.inWholeNanoseconds

            // State of passed cycles
            val cyclesPassed = currentCycle - lastStatusCycle
            val slotsAtStartOfRequest: Int
            val baseCycle: Long

            if (cyclesPassed > 0) {
                // After each period the rate limiter sets its Slots count back to the limitForPeriod value (See resilience4j)
                slotsAtStartOfRequest = data.limitForPeriod
                baseCycle = currentCycle
            } else {
                slotsAtStartOfRequest = availableSlots
                baseCycle = lastStatusCycle
            }

            val nextCycle: Long
            val nextSlots: Int
            val nanosToWait: Long

            // Case 1: Enough slots in the current/refilled window
            if (slotsAtStartOfRequest >= permits) {
                nextCycle = baseCycle
                nextSlots = slotsAtStartOfRequest - permits
                // Calculation of time until the current cycle's start point -> Determine if wait is needed
                val waitStartNanos = baseCycle * data.limitRefreshPeriod.inWholeNanoseconds
                nanosToWait = maxOf(0L, waitStartNanos - elapsedTime.inWholeNanoseconds)
            } else {
                // Case 2: "borrow" from future cycles, debt
                val deficit = permits - slotsAtStartOfRequest
                // Calculation of future cycles to satisfy the deficit
                val cyclesToWait = (deficit + data.limitForPeriod - 1) / data.limitForPeriod
                nextCycle = baseCycle + cyclesToWait
                nextSlots = (cyclesToWait * data.limitForPeriod) + slotsAtStartOfRequest - permits
                val waitStartNanos = nextCycle * data.limitRefreshPeriod.inWholeNanoseconds
                nanosToWait = waitStartNanos - elapsedTime.inWholeNanoseconds
            }

            val durationToWait = nanosToWait.nanoseconds
            // Calculated Wait is smaller then set timeout, update the shared state
            if (durationToWait <= data.timeoutDuration) {
                lastStatusCycle = nextCycle
                availableSlots = nextSlots
            }
            durationToWait
        }
    }

    // Just for checking the availability/capacity of permits
    public suspend fun getAvailableSlots(): Int {
        val now = TimeSource.Monotonic.markNow()
        val elapsedTime = now - startTime

        return mutex.withLock {
            val currentCycle = elapsedTime.inWholeNanoseconds / data.limitRefreshPeriod.inWholeNanoseconds
            val cyclesPassed = currentCycle - lastStatusCycle
            if (cyclesPassed > 0) {
                data.limitForPeriod
            } else {
                availableSlots
            }
        }
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
