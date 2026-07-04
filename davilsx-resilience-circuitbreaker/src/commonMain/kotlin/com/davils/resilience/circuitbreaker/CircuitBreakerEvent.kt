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

import com.davils.kore.pattern.reactive.event.EventMarker
import kotlin.time.Duration

/**
 * Base class for all events emitted by a [CircuitBreaker] instance.
 *
 * @since 1.0.0
 */
public sealed class CircuitBreakerEvent : EventMarker() {
    /**
     * Emitted when a call completes successfully.
     *
     * @param duration The duration of the call.
     * @since 1.0.0
     */
    public data class Success(val duration: Duration) : CircuitBreakerEvent()

    /**
     * Emitted when a call fails and the exception is recorded as a failure.
     *
     * @param duration The duration of the call before it failed.
     * @param throwable The exception that caused the failure.
     * @since 1.0.0
     */
    public data class Error(val duration: Duration, val throwable: Throwable) : CircuitBreakerEvent()

    /**
     * Emitted when a call fails but the exception is in the ignore list.
     *
     * Ignored exceptions do not count towards the failure rate.
     *
     * @param duration The duration of the call.
     * @param throwable The exception that was ignored.
     * @since 1.0.0
     */
    public data class IgnoredError(val duration: Duration, val throwable: Throwable) : CircuitBreakerEvent()

    /**
     * Emitted when a call is rejected because the circuit breaker is OPEN or FORCED_OPEN.
     *
     * @param state The current state at the time of rejection.
     * @since 1.0.0
     */
    public data class CallNotPermitted(val state: CircuitBreakerState) : CircuitBreakerEvent()

    /**
     * Emitted when the circuit breaker transitions between states.
     *
     * @param from The previous state.
     * @param to The new state.
     * @since 1.0.0
     */
    public data class StateTransition(val from: CircuitBreakerState, val to: CircuitBreakerState) : CircuitBreakerEvent()

    /**
     * Emitted when the circuit breaker is reset to CLOSED via [CircuitBreaker.reset].
     *
     * @since 1.0.0
     */
    public data object Reset : CircuitBreakerEvent()

    /**
     * Emitted when the failure rate exceeds the configured threshold, triggering the OPEN transition.
     *
     * @param failureRate The failure rate percentage at the time of the threshold breach.
     * @since 1.0.0
     */
    public data class FailureRateExceeded(val failureRate: Float) : CircuitBreakerEvent()

    /**
     * Emitted when the slow-call rate exceeds the configured threshold, triggering the OPEN transition.
     *
     * @param slowCallRate The slow-call rate percentage at the time of the threshold breach.
     * @since 1.0.0
     */
    public data class SlowCallRateExceeded(val slowCallRate: Float) : CircuitBreakerEvent()

    /**
     * Emitted when the circuit breaker instance is disposed.
     *
     * @since 1.0.0
     */
    public data object Disposed : CircuitBreakerEvent()
}
