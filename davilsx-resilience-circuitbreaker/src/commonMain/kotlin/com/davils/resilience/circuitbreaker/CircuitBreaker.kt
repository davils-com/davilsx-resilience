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

package com.davils.resilience.circuitbreaker

import com.davils.resilience.circuitbreaker.exception.CallNotPermittedException
import com.davils.resilience.circuitbreaker.exception.ResultRecordedAsFailureException
import com.davils.resilience.circuitbreaker.internal.*
import com.davils.resilience.common.ResilienceComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A circuit breaker resilience component.
 *
 * The circuit breaker prevents cascading failures by monitoring call outcomes through a
 * sliding window and temporarily blocking further calls when failure or slow-call thresholds
 * are exceeded. It implements a three-state finite state machine:
 *
 * - **CLOSED**: Normal operation. All calls are allowed and metrics are collected.
 * - **OPEN**: All calls are rejected with [CallNotPermittedException]. After the configured
 *   wait duration elapses, a transition to HALF_OPEN is attempted.
 * - **HALF_OPEN**: A limited number of probe calls are allowed. If thresholds are not exceeded,
 *   the circuit closes; otherwise it opens again.
 *
 * Additional states DISABLED, FORCED_OPEN, and METRICS_ONLY can be activated manually.
 *
 * Instances are thread-safe and can be shared across coroutines.
 *
 * @since 1.0.0
 */
public class CircuitBreaker internal constructor(
    public override val data: CircuitBreakerData,
) : ResilienceComponent<CircuitBreakerData, CircuitBreakerEvent>() {
    override val disposeEvent: CircuitBreakerEvent get() = CircuitBreakerEvent.Disposed

    // All state access is guarded by the inherited `mutex` from ResilienceComponent.
    private var stateHandler: CircuitBreakerStateHandler = buildInitialHandler()

    // Coroutine job used for automatic OPEN→HALF_OPEN and HALF_OPEN timeout transitions.
    private var autoTransitionJob: Job? = null

    // ---------------------------------------------------------------------------
    // Public API — execute
    // ---------------------------------------------------------------------------

    /**
     * Executes [block] within the circuit breaker.
     *
     * If the circuit is OPEN or FORCED_OPEN, [CallNotPermittedException] is thrown immediately
     * without invoking [block]. Otherwise the block is executed, its outcome is classified,
     * and metrics are updated accordingly.
     *
     * @param T Return type of the block.
     * @param block The operation to protect.
     * @return The result of [block].
     * @throws CallNotPermittedException If the circuit breaker is open.
     * @throws CancellationException If the circuit breaker is disposed or the coroutine is cancelled.
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        val permitted = tryAcquirePermission()
        if (!permitted) {
            val currentState = getState()
            eventBus.push(CircuitBreakerEvent.CallNotPermitted(currentState))
            throw CallNotPermittedException(currentState)
        }

        val start = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            val duration = start.elapsedNow()
            onResult(duration, result)
            result
        } catch (cancellation: CancellationException) {
            releasePermissionIfHalfOpen()
            throw cancellation
        } catch (throwable: Throwable) {
            val duration = start.elapsedNow()
            onError(duration, throwable)
            throw throwable
        }
    }

    // ---------------------------------------------------------------------------
    // Public API — permission lifecycle
    // ---------------------------------------------------------------------------

    /**
     * Attempts to acquire a permission to make a call.
     *
     * Returns `true` if a call is permitted. For OPEN/FORCED_OPEN states this returns `false`
     * without throwing. For OPEN state, if the wait duration has elapsed, the circuit
     * transitions to HALF_OPEN first.
     *
     * @since 1.0.0
     */
    public suspend fun tryAcquirePermission(): Boolean = mutex.withLock {
        val handler = stateHandler
        if (handler is OpenStateHandler && handler.isWaitElapsed()) {
            transitionToHalfOpenUnsafe(handler.attempts)
            stateHandler.tryAcquirePermission()
        } else {
            handler.tryAcquirePermission()
        }
    }

    /**
     * Acquires a permission or throws [CallNotPermittedException] if not permitted.
     *
     * @since 1.0.0
     */
    public suspend fun acquirePermission() {
        if (!tryAcquirePermission()) {
            val state = getState()
            eventBus.push(CircuitBreakerEvent.CallNotPermitted(state))
            throw CallNotPermittedException(state)
        }
    }

    /**
     * Releases a previously acquired permission without recording any outcome.
     *
     * Only meaningful in HALF_OPEN state where the slot is returned to the probe quota.
     *
     * @since 1.0.0
     */
    public suspend fun releasePermission(): Unit = mutex.withLock {
        releasePermissionUnsafe()
    }

    // ---------------------------------------------------------------------------
    // Public API — recording outcomes manually
    // ---------------------------------------------------------------------------

    /**
     * Records a successful call outcome with the given [duration].
     *
     * @since 1.0.0
     */
    public suspend fun onSuccess(duration: Duration) {
        mutex.withLock { recordSuccessUnsafe(duration) }
    }

    /**
     * Records a failed call outcome.
     *
     * If [throwable] is matched by the ignore predicate it is counted as ignored; if it matches
     * the record predicate it is counted as a failure; otherwise it is treated as a success.
     *
     * @since 1.0.0
     */
    public suspend fun onError(duration: Duration, throwable: Throwable) {
        if (data.ignoreExceptionPredicate.test(throwable)) {
            eventBus.push(CircuitBreakerEvent.IgnoredError(duration, throwable))
            return
        }
        if (data.recordExceptionPredicate.test(throwable)) {
            eventBus.push(CircuitBreakerEvent.Error(duration, throwable))
            mutex.withLock {
                val result = stateHandler.onError(duration)
                handleThresholdResultUnsafe(result)
            }
        } else {
            mutex.withLock { recordSuccessUnsafe(duration) }
        }
    }

    /**
     * Records the [result] of a successful call.
     *
     * If [result] matches the `recordResultPredicate`, a [ResultRecordedAsFailureException] is
     * generated and the call is counted as a failure.
     *
     * @since 1.0.0
     */
    public suspend fun onResult(duration: Duration, result: Any?) {
        if (data.recordResultPredicate.test(result)) {
            val failure = ResultRecordedAsFailureException(result)
            eventBus.push(CircuitBreakerEvent.Error(duration, failure))
            mutex.withLock {
                val thresholdResult = stateHandler.onError(duration)
                handleThresholdResultUnsafe(thresholdResult)
            }
        } else {
            mutex.withLock { recordSuccessUnsafe(duration) }
        }
    }

    // ---------------------------------------------------------------------------
    // Public API — state queries
    // ---------------------------------------------------------------------------

    /**
     * Returns the current state of the circuit breaker.
     *
     * @since 1.0.0
     */
    public suspend fun getState(): CircuitBreakerState = mutex.withLock { stateHandler.state }

    /**
     * Returns a snapshot of the current metrics.
     *
     * @since 1.0.0
     */
    public suspend fun getMetrics(): CircuitBreakerMetrics = mutex.withLock { stateHandler.metrics() }

    // ---------------------------------------------------------------------------
    // Public API — manual state transitions
    // ---------------------------------------------------------------------------

    /**
     * Resets the circuit breaker to CLOSED and clears all recorded metrics.
     *
     * @since 1.0.0
     */
    public suspend fun reset() {
        mutex.withLock {
            val prev = stateHandler.state
            cancelAutoTransitionJob()
            stateHandler = ClosedStateHandler(data)
            if (prev != CircuitBreakerState.CLOSED) {
                eventBus.push(CircuitBreakerEvent.StateTransition(prev, CircuitBreakerState.CLOSED))
            }
        }
        eventBus.push(CircuitBreakerEvent.Reset)
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.CLOSED].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToClosed(): Unit = performTransition(CircuitBreakerState.CLOSED) {
        ClosedStateHandler(data)
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.OPEN].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToOpen(): Unit = performTransition(CircuitBreakerState.OPEN) {
        val attempts = (stateHandler as? OpenStateHandler)?.attempts?.plus(1) ?: 1
        OpenStateHandler(data, attempts, stateHandler.metrics())
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.HALF_OPEN].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToHalfOpen(): Unit = mutex.withLock {
        val prev = stateHandler.state
        val attempts = (stateHandler as? OpenStateHandler)?.attempts ?: 1
        cancelAutoTransitionJob()
        val newHandler = HalfOpenStateHandler(data, attempts)
        stateHandler = newHandler
        scheduleHalfOpenTimeoutIfNeeded(newHandler)
        eventBus.push(CircuitBreakerEvent.StateTransition(prev, CircuitBreakerState.HALF_OPEN))
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.DISABLED].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToDisabled(): Unit = performTransition(CircuitBreakerState.DISABLED) {
        DisabledStateHandler(data)
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.FORCED_OPEN].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToForcedOpen(): Unit = performTransition(CircuitBreakerState.FORCED_OPEN) {
        ForcedOpenStateHandler(data)
    }

    /**
     * Manually transitions the circuit breaker to [CircuitBreakerState.METRICS_ONLY].
     *
     * @since 1.0.0
     */
    public suspend fun transitionToMetricsOnly(): Unit = performTransition(CircuitBreakerState.METRICS_ONLY) {
        MetricsOnlyStateHandler(data)
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun buildInitialHandler(): CircuitBreakerStateHandler = when (data.initialState) {
        CircuitBreakerState.CLOSED -> ClosedStateHandler(data)
        CircuitBreakerState.OPEN -> OpenStateHandler(data, 1, emptyMetrics())
        CircuitBreakerState.HALF_OPEN -> HalfOpenStateHandler(data, 1)
        CircuitBreakerState.DISABLED -> DisabledStateHandler(data)
        CircuitBreakerState.FORCED_OPEN -> ForcedOpenStateHandler(data)
        CircuitBreakerState.METRICS_ONLY -> MetricsOnlyStateHandler(data)
    }

    /** Must be called under the mutex. */
    private fun recordSuccessUnsafe(duration: Duration) {
        val result = stateHandler.onSuccess(duration)
        val successEvent = CircuitBreakerEvent.Success(duration)
        eventBus.push(successEvent)
        handleThresholdResultUnsafe(result)
        checkHalfOpenCompletionUnsafe()
    }

    /** Must be called under the mutex. */
    private fun releasePermissionUnsafe() {
        (stateHandler as? HalfOpenStateHandler)?.releasePermission()
    }

    private suspend fun releasePermissionIfHalfOpen(): Unit = mutex.withLock { releasePermissionUnsafe() }

    /** Must be called under the mutex. */
    private fun handleThresholdResultUnsafe(result: ThresholdResult) {
        if (!result.anyExceeded) {
            checkHalfOpenCompletionUnsafe()
            return
        }
        val currentState = stateHandler.state
        if (currentState != CircuitBreakerState.CLOSED && currentState != CircuitBreakerState.HALF_OPEN) return

        // Emit threshold events before the state transition
        if (result.hasFailureRateExceeded) {
            eventBus.push(CircuitBreakerEvent.FailureRateExceeded(stateHandler.metrics().failureRate))
        }
        if (result.hasSlowCallRateExceeded) {
            eventBus.push(CircuitBreakerEvent.SlowCallRateExceeded(stateHandler.metrics().slowCallRate))
        }

        val newAttempts = (stateHandler as? OpenStateHandler)?.attempts?.plus(1) ?: 1
        val metrics = stateHandler.metrics()
        cancelAutoTransitionJob()
        stateHandler = OpenStateHandler(data, newAttempts, metrics)
        eventBus.push(CircuitBreakerEvent.StateTransition(currentState, CircuitBreakerState.OPEN))

        if (data.automaticTransitionFromOpenToHalfOpen) {
            scheduleAutoTransitionToHalfOpen(stateHandler as OpenStateHandler)
        }
    }

    /** Must be called under the mutex. */
    private fun checkHalfOpenCompletionUnsafe() {
        val handler = stateHandler as? HalfOpenStateHandler ?: return
        if (handler.allProbeCallsSucceeded()) {
            cancelAutoTransitionJob()
            stateHandler = ClosedStateHandler(data)
            eventBus.push(
                CircuitBreakerEvent.StateTransition(
                    CircuitBreakerState.HALF_OPEN,
                    CircuitBreakerState.CLOSED
                )
            )
        }
    }

    /** Must be called under the mutex. */
    private fun transitionToHalfOpenUnsafe(previousAttempts: Int) {
        cancelAutoTransitionJob()
        val newHandler = HalfOpenStateHandler(data, previousAttempts)
        stateHandler = newHandler
        scheduleHalfOpenTimeoutIfNeeded(newHandler)
        eventBus.push(CircuitBreakerEvent.StateTransition(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN))
    }

    private fun scheduleAutoTransitionToHalfOpen(openHandler: OpenStateHandler) {
        val waitDuration = data.waitIntervalInOpenState.waitDuration(openHandler.attempts)
        autoTransitionJob = data.eventData.scope.launch {
            delay(waitDuration)
            if (isActive) {
                mutex.withLock {
                    if (stateHandler === openHandler) {
                        transitionToHalfOpenUnsafe(openHandler.attempts)
                    }
                }
            }
        }
    }

    private fun scheduleHalfOpenTimeoutIfNeeded(handler: HalfOpenStateHandler) {
        if (data.maxWaitDurationInHalfOpenState <= Duration.ZERO) return
        autoTransitionJob = data.eventData.scope.launch {
            delay(data.maxWaitDurationInHalfOpenState)
            if (isActive) {
                mutex.withLock {
                    if (stateHandler === handler) {
                        val targetState = data.transitionStateAfterWaitDuration
                        cancelAutoTransitionJob()
                        stateHandler = when (targetState) {
                            CircuitBreakerState.OPEN -> OpenStateHandler(data, handler.attempts + 1, handler.metrics())
                            CircuitBreakerState.CLOSED -> ClosedStateHandler(data)
                            else -> ClosedStateHandler(data)
                        }
                        eventBus.push(CircuitBreakerEvent.StateTransition(CircuitBreakerState.HALF_OPEN, targetState))
                    }
                }
            }
        }
    }

    private fun cancelAutoTransitionJob() {
        autoTransitionJob?.cancel()
        autoTransitionJob = null
    }

    private suspend fun performTransition(
        target: CircuitBreakerState,
        buildHandler: () -> CircuitBreakerStateHandler
    ) {
        mutex.withLock {
            val prev = stateHandler.state
            cancelAutoTransitionJob()
            stateHandler = buildHandler()
            if (prev != target) {
                eventBus.push(CircuitBreakerEvent.StateTransition(prev, target))
            }
        }
    }

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
}

/**
 * Creates a new [CircuitBreaker] configured by [builder].
 *
 * @param builder Configuration block applied to a [CircuitBreakerBuilder].
 * @return A fully configured [CircuitBreaker] instance.
 * @since 1.0.0
 */
public fun circuitBreaker(builder: CircuitBreakerBuilder.() -> Unit): CircuitBreaker {
    val b = CircuitBreakerBuilder()
    b.apply(builder)
    val data = b.produce()
    return CircuitBreaker(data)
}
