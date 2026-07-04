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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

/**
 * A resilience component that limits the rate of operations over a configurable time window.
 *
 * Supports fixed and sliding window algorithms, configurable acquire strategies, and runtime
 * limit adjustments. When the configured limit is exceeded, callers may wait up to
 * [RateLimiterData.timeoutDuration] or be rejected with [RequestNotPermittedException].
 *
 * @since 1.0.0
 */
public class RateLimiter internal constructor(
    public override val data: RateLimiterData,
) : ResilienceComponent<RateLimiterData, RateLimiterEvent>() {

    override val disposeEvent: RateLimiterEvent
        get() = RateLimiterEvent.RateLimiterDisposed

    private val startTime = TimeSource.Monotonic.markNow()
    private var lastStatusCycle: Long = 0L
    private var availableSlots: Int = data.limitForPeriod
    private val slidingTimestamps = mutableListOf<Long>()

    private var runtimeLimitForPeriod: Int = data.limitForPeriod
    private var runtimeTimeoutDuration: Duration = data.timeoutDuration

    private val successfulAcquires = atomic(0L)
    private val failedAcquires = atomic(0L)
    private val totalWaitNanos = atomic(0L)
    private val waitingThreads = atomic(0)

    /**
     * Executes [block] after acquiring a single permit.
     *
     * @throws RequestNotPermittedException when a permit cannot be acquired within the configured timeout.
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T = execute(1, block)

    /**
     * Executes [block] after acquiring the requested number of permits.
     *
     * @param permits Number of permits to consume. Must not exceed [runtimeLimitForPeriod].
     * @throws RequestNotPermittedException when permits cannot be acquired within the configured timeout.
     * @throws IllegalArgumentException when [permits] exceeds the configured limit.
     * @since 1.0.0
     */
    public suspend fun <T> execute(permits: Int, block: suspend () -> T): T {
        if (acquireSlot(permits)) {
            return block()
        }
        throw RequestNotPermittedException()
    }

    /**
     * Attempts to acquire [permits], waiting according to the configured [RateLimiterData.strategy].
     *
     * @return `true` when permits were acquired, `false` when the request was rejected.
     * @since 1.0.0
     */
    public suspend fun acquireSlot(permits: Int = 1): Boolean {
        checkDisposal()
        waitingThreads.incrementAndGet()
        return try {
            val durationToWait = reserveSlotInternal(permits) { waitDuration ->
                isWaitPermitted(waitDuration)
            }
            if (isWaitPermitted(durationToWait)) {
                if (durationToWait > Duration.ZERO) {
                    delay(durationToWait)
                    totalWaitNanos.addAndGet(durationToWait.inWholeNanoseconds)
                }
                successfulAcquires.incrementAndGet()
                eventBus.push(RateLimiterEvent.SuccessfulAcquire(permits))
                true
            } else {
                failedAcquires.incrementAndGet()
                eventBus.push(RateLimiterEvent.FailedAcquire(permits, durationToWait))
                false
            }
        } finally {
            waitingThreads.decrementAndGet()
        }
    }

    /**
     * Attempts to acquire [permits] without waiting.
     *
     * @return `true` when permits were available immediately, `false` otherwise.
     * @since 1.0.0
     */
    public suspend fun tryAcquire(permits: Int = 1): Boolean {
        checkDisposal()
        val durationToWait = reserveSlotInternal(permits) { waitDuration ->
            waitDuration <= Duration.ZERO
        }
        if (durationToWait <= Duration.ZERO) {
            successfulAcquires.incrementAndGet()
            eventBus.push(RateLimiterEvent.SuccessfulAcquire(permits))
            return true
        }
        failedAcquires.incrementAndGet()
        eventBus.push(RateLimiterEvent.FailedAcquire(permits, durationToWait))
        return false
    }

    /**
     * Calculates the wait duration required to acquire [permits] without consuming them.
     *
     * @since 1.0.0
     */
    public suspend fun reserveSlot(permits: Int = 1): Duration {
        checkDisposal()
        return reserveSlotInternal(permits) { false }
    }

    /**
     * Returns the number of permits currently available in the active window.
     *
     * @since 1.0.0
     */
    public suspend fun getAvailableSlots(): Int {
        val now = TimeSource.Monotonic.markNow()
        val elapsedTime = now - startTime

        return mutex.withLock {
            when (data.windowType) {
                RateLimiterWindowType.FIXED -> availableSlotsFixed(elapsedTime)
                RateLimiterWindowType.SLIDING -> availableSlotsSliding(elapsedTime.inWholeNanoseconds)
            }
        }
    }

    /**
     * Returns a snapshot of the current metrics.
     *
     * @since 1.0.0
     */
    public suspend fun getMetrics(): RateLimiterMetrics = mutex.withLock {
        val elapsedTime = TimeSource.Monotonic.markNow() - startTime
        val available = when (data.windowType) {
            RateLimiterWindowType.FIXED -> availableSlotsFixed(elapsedTime)
            RateLimiterWindowType.SLIDING -> availableSlotsSliding(elapsedTime.inWholeNanoseconds)
        }
        RateLimiterMetrics(
            availablePermissions = available,
            limitForPeriod = runtimeLimitForPeriod,
            limitRefreshPeriod = data.limitRefreshPeriod,
            numberOfWaitingThreads = waitingThreads.value,
            numberOfSuccessfulAcquires = successfulAcquires.value,
            numberOfFailedAcquires = failedAcquires.value,
            totalWaitTime = totalWaitNanos.value.nanoseconds,
        )
    }

    /**
     * Updates the maximum number of permits per refresh period at runtime.
     *
     * @since 1.0.0
     */
    public suspend fun changeLimitForPeriod(limitForPeriod: Int) {
        require(limitForPeriod >= 1) { "limitForPeriod must be at least 1" }
        mutex.withLock {
            val previousLimit = runtimeLimitForPeriod
            runtimeLimitForPeriod = limitForPeriod
            availableSlots = (availableSlots + (limitForPeriod - previousLimit)).coerceAtMost(limitForPeriod)
            while (slidingTimestamps.size > limitForPeriod) {
                slidingTimestamps.removeAt(0)
            }
        }
    }

    /**
     * Updates the maximum wait duration at runtime.
     *
     * @since 1.0.0
     */
    public suspend fun changeTimeoutDuration(timeoutDuration: Duration) {
        require(!timeoutDuration.isNegative()) { "timeoutDuration must be non-negative" }
        mutex.withLock {
            runtimeTimeoutDuration = timeoutDuration
        }
    }

    private fun isWaitPermitted(waitDuration: Duration): Boolean = when (data.strategy) {
        RateLimiterStrategy.FAIL_FAST -> waitDuration <= Duration.ZERO
        RateLimiterStrategy.WAIT -> waitDuration <= runtimeTimeoutDuration
        RateLimiterStrategy.BLOCKING -> true
    }

    private suspend fun reserveSlotInternal(
        permits: Int,
        acceptWait: (Duration) -> Boolean,
    ): Duration {
        require(permits <= runtimeLimitForPeriod) {
            "Requested permits ($permits) cannot be greater than limitForPeriod ($runtimeLimitForPeriod)"
        }

        val now = TimeSource.Monotonic.markNow()
        val elapsedTime = now - startTime

        return mutex.withLock {
            when (data.windowType) {
                RateLimiterWindowType.FIXED -> reserveFixedWindow(permits, elapsedTime, acceptWait)
                RateLimiterWindowType.SLIDING -> reserveSlidingWindow(permits, elapsedTime.inWholeNanoseconds, acceptWait)
            }
        }
    }

    private fun availableSlotsFixed(elapsedTime: Duration): Int {
        val currentCycle = elapsedTime.inWholeNanoseconds / data.limitRefreshPeriod.inWholeNanoseconds
        val cyclesPassed = currentCycle - lastStatusCycle
        return if (cyclesPassed > 0) {
            runtimeLimitForPeriod
        } else {
            availableSlots
        }
    }

    private fun availableSlotsSliding(nowNanos: Long): Int {
        pruneSlidingWindow(nowNanos)
        return (runtimeLimitForPeriod - slidingTimestamps.size).coerceAtLeast(0)
    }

    private fun pruneSlidingWindow(nowNanos: Long) {
        val windowNanos = data.limitRefreshPeriod.inWholeNanoseconds
        slidingTimestamps.removeAll { nowNanos - it >= windowNanos }
    }

    private fun reserveFixedWindow(
        permits: Int,
        elapsedTime: Duration,
        acceptWait: (Duration) -> Boolean,
    ): Duration {
        val currentCycle = elapsedTime.inWholeNanoseconds / data.limitRefreshPeriod.inWholeNanoseconds
        val cyclesPassed = currentCycle - lastStatusCycle
        val slotsAtStartOfRequest: Int
        val baseCycle: Long

        if (cyclesPassed > 0) {
            slotsAtStartOfRequest = runtimeLimitForPeriod
            baseCycle = currentCycle
        } else {
            slotsAtStartOfRequest = availableSlots
            baseCycle = lastStatusCycle
        }

        val nextCycle: Long
        val nextSlots: Int
        val nanosToWait: Long

        if (slotsAtStartOfRequest >= permits) {
            nextCycle = baseCycle
            nextSlots = slotsAtStartOfRequest - permits
            val waitStartNanos = baseCycle * data.limitRefreshPeriod.inWholeNanoseconds
            nanosToWait = maxOf(0L, waitStartNanos - elapsedTime.inWholeNanoseconds)
        } else {
            val deficit = permits - slotsAtStartOfRequest
            val cyclesToWait = (deficit + runtimeLimitForPeriod - 1) / runtimeLimitForPeriod
            nextCycle = baseCycle + cyclesToWait
            nextSlots = (cyclesToWait * runtimeLimitForPeriod) + slotsAtStartOfRequest - permits
            val waitStartNanos = nextCycle * data.limitRefreshPeriod.inWholeNanoseconds
            nanosToWait = waitStartNanos - elapsedTime.inWholeNanoseconds
        }

        val durationToWait = nanosToWait.nanoseconds
        if (acceptWait(durationToWait)) {
            lastStatusCycle = nextCycle
            availableSlots = nextSlots
        }
        return durationToWait
    }

    private fun reserveSlidingWindow(
        permits: Int,
        nowNanos: Long,
        acceptWait: (Duration) -> Boolean,
    ): Duration {
        pruneSlidingWindow(nowNanos)
        val activeCount = slidingTimestamps.size

        if (activeCount + permits <= runtimeLimitForPeriod) {
            if (acceptWait(Duration.ZERO)) {
                repeat(permits) {
                    slidingTimestamps.add(nowNanos)
                }
            }
            return Duration.ZERO
        }

        val overflow = activeCount + permits - runtimeLimitForPeriod
        val waitUntilNanos = slidingTimestamps[overflow - 1] + data.limitRefreshPeriod.inWholeNanoseconds
        val nanosToWait = (waitUntilNanos - nowNanos).coerceAtLeast(0L)
        val durationToWait = nanosToWait.nanoseconds

        if (acceptWait(durationToWait)) {
            pruneSlidingWindow(waitUntilNanos)
            repeat(permits) {
                slidingTimestamps.add(waitUntilNanos)
            }
        }
        return durationToWait
    }

    private suspend fun checkDisposal() {
        if (isDisposed()) {
            throw CancellationException("RateLimiter instance is disposed")
        }
    }
}

/**
 * Creates a [RateLimiter] using the DSL builder.
 *
 * @since 1.0.0
 */
public fun rateLimiter(builder: RateLimiterBuilder.() -> Unit): RateLimiter {
    val rateLimiterBuilder = RateLimiterBuilder()
    rateLimiterBuilder.builder()
    val data = rateLimiterBuilder.produce()
    return RateLimiter(data)
}
