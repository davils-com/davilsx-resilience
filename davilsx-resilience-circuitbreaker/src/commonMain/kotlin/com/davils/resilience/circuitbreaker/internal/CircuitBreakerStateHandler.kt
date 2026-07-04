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

package com.davils.resilience.circuitbreaker.internal

import com.davils.resilience.circuitbreaker.CircuitBreakerData
import com.davils.resilience.circuitbreaker.CircuitBreakerMetrics
import com.davils.resilience.circuitbreaker.CircuitBreakerState
import com.davils.resilience.circuitbreaker.SlidingWindowType
import kotlinx.atomicfu.atomic
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Internal result after recording a call outcome into the sliding window.
 *
 * Used by the [com.davils.resilience.circuitbreaker.CircuitBreaker] to decide whether to emit threshold-exceeded events and
 * trigger a state transition.
 */
internal enum class ThresholdResult {
    BELOW_MINIMUM,
    BELOW_THRESHOLDS,
    FAILURE_RATE_EXCEEDED,
    SLOW_CALL_RATE_EXCEEDED,
    BOTH_EXCEEDED;

    val hasFailureRateExceeded: Boolean
        get() = this == FAILURE_RATE_EXCEEDED || this == BOTH_EXCEEDED
    val hasSlowCallRateExceeded: Boolean
        get() = this == SLOW_CALL_RATE_EXCEEDED || this == BOTH_EXCEEDED
    val anyExceeded: Boolean
        get() = this != BELOW_MINIMUM && this != BELOW_THRESHOLDS
}

/**
 * Internal state object held by [com.davils.resilience.circuitbreaker.CircuitBreaker].
 *
 * Every field of this class is accessed while the circuit breaker's mutex is held, so
 * no additional synchronization is needed inside the handler itself. The only exception
 * is [notPermittedCount], which is an atomic because it may be incremented from the
 * fast path without the mutex in some implementations.
 */
internal sealed class CircuitBreakerStateHandler(
    protected val data: CircuitBreakerData,
) {
    internal val notPermittedCount = atomic(0L)

    abstract val state: CircuitBreakerState

    /** Returns true if a call is allowed to proceed. Must be called under the mutex. */
    open fun tryAcquirePermission(): Boolean = true

    /**
     * Records a successful call. Returns a [ThresholdResult] so the circuit breaker
     * can decide whether to open. Must be called under the mutex.
     */
    open fun onSuccess(duration: Duration): ThresholdResult = ThresholdResult.BELOW_THRESHOLDS

    /**
     * Records a failed call. Returns a [ThresholdResult]. Must be called under the mutex.
     */
    open fun onError(duration: Duration): ThresholdResult = ThresholdResult.BELOW_THRESHOLDS

    /** Builds the public [CircuitBreakerMetrics] snapshot. Must be called under the mutex. */
    abstract fun metrics(): CircuitBreakerMetrics

    /** Called just before the handler is replaced with a new one. */
    open fun preTransition() {}

    /** Increments the not-permitted counter and signals that the call must be rejected. */
    protected fun rejectCall(): Boolean {
        notPermittedCount.incrementAndGet()
        return false
    }
}

/**
 * Base handler for states that record call outcomes in a sliding window.
 */
internal abstract class SlidingWindowStateHandler(
    data: CircuitBreakerData,
    protected val window: SlidingWindowMetrics,
    private val enforceThresholds: Boolean,
) : CircuitBreakerStateHandler(data) {

    protected abstract fun minimumCallsForEvaluation(): Int

    protected fun windowSnapshot(): WindowSnapshot = window.snapshot()

    override fun onSuccess(duration: Duration): ThresholdResult =
        recordCall(duration, isSuccess = true)

    override fun onError(duration: Duration): ThresholdResult =
        recordCall(duration, isSuccess = false)

    override fun metrics(): CircuitBreakerMetrics {
        val minCalls = minimumCallsForEvaluation()
        return metricsFromSnapshot(window.snapshot(), minCalls, notPermittedCount.value)
    }

    private fun recordCall(duration: Duration, isSuccess: Boolean): ThresholdResult {
        val outcome = callOutcome(duration, isSuccess, data.slowCallDurationThreshold)
        window.record(outcome, duration)
        if (!enforceThresholds) return ThresholdResult.BELOW_THRESHOLDS
        return evaluateThresholds(window.snapshot(), minimumCallsForEvaluation(), data)
    }
}

// ---------------------------------------------------------------------------
// CLOSED — normal operation
// ---------------------------------------------------------------------------

internal class ClosedStateHandler(data: CircuitBreakerData) : SlidingWindowStateHandler(
    data = data,
    window = buildWindow(data),
    enforceThresholds = true,
) {
    override val state: CircuitBreakerState = CircuitBreakerState.CLOSED

    override fun minimumCallsForEvaluation(): Int = data.minimumCallsForSlidingWindow()
}

// ---------------------------------------------------------------------------
// OPEN — reject all calls
// ---------------------------------------------------------------------------

internal class OpenStateHandler(
    data: CircuitBreakerData,
    internal val attempts: Int,
    previousMetrics: CircuitBreakerMetrics,
) : CircuitBreakerStateHandler(data) {
    override val state: CircuitBreakerState = CircuitBreakerState.OPEN

    private val retryAfter: TimeSource.Monotonic.ValueTimeMark
    private val savedMetrics: CircuitBreakerMetrics

    init {
        val waitDuration = data.waitIntervalInOpenState.waitDuration(attempts)
        retryAfter = TimeSource.Monotonic.markNow() + waitDuration
        savedMetrics = previousMetrics
    }

    override fun tryAcquirePermission(): Boolean = rejectCall()

    /** Returns `true` when the wait duration has elapsed and the state should move to HALF_OPEN. */
    fun isWaitElapsed(): Boolean = TimeSource.Monotonic.markNow() >= retryAfter

    override fun metrics(): CircuitBreakerMetrics = savedMetrics.copy(
        numberOfNotPermittedCalls = savedMetrics.numberOfNotPermittedCalls + notPermittedCount.value
    )
}

// ---------------------------------------------------------------------------
// HALF_OPEN — limited probe calls
// ---------------------------------------------------------------------------

internal class HalfOpenStateHandler(
    data: CircuitBreakerData,
    internal val attempts: Int,
) : SlidingWindowStateHandler(
    data = data,
    window = CountBasedSlidingWindow(data.permittedCallsInHalfOpenState),
    enforceThresholds = true,
) {
    override val state: CircuitBreakerState = CircuitBreakerState.HALF_OPEN

    private val permitQuota = atomic(data.permittedCallsInHalfOpenState)

    override fun minimumCallsForEvaluation(): Int = data.permittedCallsInHalfOpenState

    override fun tryAcquirePermission(): Boolean {
        while (true) {
            val current = permitQuota.value
            if (current <= 0) {
                notPermittedCount.incrementAndGet()
                return false
            }
            if (permitQuota.compareAndSet(current, current - 1)) return true
        }
    }

    /** Release a previously acquired permit (e.g. on cancellation). */
    fun releasePermission() {
        permitQuota.incrementAndGet()
    }

    /** True when all probe calls have been evaluated and thresholds are still within limits. */
    fun allProbeCallsSucceeded(): Boolean {
        val snap = windowSnapshot()
        return snap.totalCalls >= data.permittedCallsInHalfOpenState &&
            !evaluateThresholds(snap, minimumCallsForEvaluation(), data).anyExceeded
    }
}

// ---------------------------------------------------------------------------
// DISABLED — all calls pass, no metrics
// ---------------------------------------------------------------------------

internal class DisabledStateHandler(data: CircuitBreakerData) : CircuitBreakerStateHandler(data) {
    override val state: CircuitBreakerState = CircuitBreakerState.DISABLED

    override fun metrics(): CircuitBreakerMetrics = emptyMetrics()
}

// ---------------------------------------------------------------------------
// FORCED_OPEN — all calls rejected
// ---------------------------------------------------------------------------

internal class ForcedOpenStateHandler(data: CircuitBreakerData) : CircuitBreakerStateHandler(data) {
    override val state: CircuitBreakerState = CircuitBreakerState.FORCED_OPEN

    override fun tryAcquirePermission(): Boolean = rejectCall()

    override fun metrics(): CircuitBreakerMetrics = emptyMetrics().copy(
        numberOfNotPermittedCalls = notPermittedCount.value
    )
}

// ---------------------------------------------------------------------------
// METRICS_ONLY — all calls pass, metrics recorded, thresholds never enforced
// ---------------------------------------------------------------------------

internal class MetricsOnlyStateHandler(data: CircuitBreakerData) : SlidingWindowStateHandler(
    data = data,
    window = buildWindow(data),
    enforceThresholds = false,
) {
    override val state: CircuitBreakerState = CircuitBreakerState.METRICS_ONLY

    override fun minimumCallsForEvaluation(): Int = data.minimumCallsForSlidingWindow()
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun buildWindow(data: CircuitBreakerData): SlidingWindowMetrics =
    when (data.slidingWindowType) {
        SlidingWindowType.COUNT_BASED -> CountBasedSlidingWindow(data.slidingWindowSize)
        SlidingWindowType.TIME_BASED -> TimeBasedSlidingWindow(data.slidingWindowSize)
    }

private fun CircuitBreakerData.minimumCallsForSlidingWindow(): Int =
    if (slidingWindowType == SlidingWindowType.COUNT_BASED) {
        minOf(minimumNumberOfCalls, slidingWindowSize)
    } else {
        minimumNumberOfCalls
    }

private fun callOutcome(duration: Duration, isSuccess: Boolean, slowCallThreshold: Duration): CallOutcome =
    when {
        isSuccess && duration > slowCallThreshold -> CallOutcome.SLOW_SUCCESS
        isSuccess -> CallOutcome.SUCCESS
        duration > slowCallThreshold -> CallOutcome.SLOW_ERROR
        else -> CallOutcome.ERROR
    }

private fun evaluateThresholds(
    snap: WindowSnapshot,
    minCalls: Int,
    data: CircuitBreakerData,
): ThresholdResult {
    if (snap.totalCalls < minCalls) return ThresholdResult.BELOW_MINIMUM

    val failureRate = snap.failureRate(minCalls)
    val slowRate = snap.slowCallRate(minCalls)
    val failureExceeded = failureRate >= 0 && failureRate >= data.failureRateThreshold
    val slowExceeded = slowRate >= 0 && slowRate >= data.slowCallRateThreshold

    return when {
        failureExceeded && slowExceeded -> ThresholdResult.BOTH_EXCEEDED
        failureExceeded -> ThresholdResult.FAILURE_RATE_EXCEEDED
        slowExceeded -> ThresholdResult.SLOW_CALL_RATE_EXCEEDED
        else -> ThresholdResult.BELOW_THRESHOLDS
    }
}

private fun metricsFromSnapshot(
    snap: WindowSnapshot,
    minCalls: Int,
    notPermittedCount: Long,
): CircuitBreakerMetrics = CircuitBreakerMetrics(
    numberOfBufferedCalls = snap.totalCalls,
    numberOfSuccessfulCalls = snap.successCalls,
    numberOfFailedCalls = snap.failureCalls,
    numberOfSlowCalls = snap.slowCalls,
    numberOfSlowSuccessfulCalls = snap.slowSuccessCalls,
    numberOfSlowFailedCalls = snap.slowErrorCalls,
    numberOfNotPermittedCalls = notPermittedCount,
    failureRate = snap.failureRate(minCalls),
    slowCallRate = snap.slowCallRate(minCalls),
)

private fun emptyMetrics() = CircuitBreakerMetrics(
    numberOfBufferedCalls = 0,
    numberOfSuccessfulCalls = 0,
    numberOfFailedCalls = 0,
    numberOfSlowCalls = 0,
    numberOfSlowSuccessfulCalls = 0,
    numberOfSlowFailedCalls = 0,
    numberOfNotPermittedCalls = 0L,
    failureRate = -1f,
    slowCallRate = -1f,
)
