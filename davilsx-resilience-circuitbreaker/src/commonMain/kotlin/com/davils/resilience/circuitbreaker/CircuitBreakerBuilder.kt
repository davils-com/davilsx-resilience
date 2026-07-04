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

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.circuitbreaker.predicate.*
import com.davils.resilience.circuitbreaker.strategy.WaitIntervalStrategy
import com.davils.resilience.circuitbreaker.strategy.fixedWaitInterval
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration

/**
 * Builder for configuring and creating [CircuitBreakerData] instances.
 *
 * Provides a DSL-friendly API with defaults matching resilience4j's defaults.
 *
 * @since 1.0.0
 */
@KoreDsl
public class CircuitBreakerBuilder internal constructor() : ResilienceComponentBuilder<CircuitBreakerData>() {
    /**
     * Failure rate threshold in percent (0–100).
     * Defaults to [CircuitBreakerData.DEFAULT_FAILURE_RATE_THRESHOLD].
     *
     * @since 1.0.0
     */
    public var failureRateThreshold: Float = CircuitBreakerData.DEFAULT_FAILURE_RATE_THRESHOLD

    /**
     * Slow-call rate threshold in percent (0–100).
     * Defaults to [CircuitBreakerData.DEFAULT_SLOW_CALL_RATE_THRESHOLD].
     *
     * @since 1.0.0
     */
    public var slowCallRateThreshold: Float = CircuitBreakerData.DEFAULT_SLOW_CALL_RATE_THRESHOLD

    /**
     * Duration above which a call is considered slow.
     * Defaults to [CircuitBreakerData.DEFAULT_SLOW_CALL_DURATION_THRESHOLD].
     *
     * @since 1.0.0
     */
    public var slowCallDurationThreshold: Duration = CircuitBreakerData.DEFAULT_SLOW_CALL_DURATION_THRESHOLD

    /**
     * Strategy controlling how long the circuit stays OPEN.
     * Defaults to a fixed 60-second wait.
     *
     * @since 1.0.0
     */
    public var waitIntervalInOpenState: WaitIntervalStrategy = CircuitBreakerData.DEFAULT_WAIT_INTERVAL_IN_OPEN_STATE

    /**
     * Number of probe calls allowed in HALF_OPEN state.
     * Defaults to [CircuitBreakerData.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE].
     *
     * @since 1.0.0
     */
    public var permittedCallsInHalfOpenState: Int = CircuitBreakerData.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE

    /**
     * Minimum calls before thresholds are evaluated.
     * Defaults to [CircuitBreakerData.DEFAULT_MINIMUM_NUMBER_OF_CALLS].
     *
     * @since 1.0.0
     */
    public var minimumNumberOfCalls: Int = CircuitBreakerData.DEFAULT_MINIMUM_NUMBER_OF_CALLS

    /**
     * Size of the sliding window (calls for COUNT_BASED, seconds for TIME_BASED).
     * Defaults to [CircuitBreakerData.DEFAULT_SLIDING_WINDOW_SIZE].
     *
     * @since 1.0.0
     */
    public var slidingWindowSize: Int = CircuitBreakerData.DEFAULT_SLIDING_WINDOW_SIZE

    /**
     * Type of sliding window.
     * Defaults to [CircuitBreakerData.DEFAULT_SLIDING_WINDOW_TYPE].
     *
     * @since 1.0.0
     */
    public var slidingWindowType: SlidingWindowType = CircuitBreakerData.DEFAULT_SLIDING_WINDOW_TYPE

    /**
     * Whether to automatically transition from OPEN to HALF_OPEN after the wait duration.
     * Defaults to `false`.
     *
     * @since 1.0.0
     */
    public var automaticTransitionFromOpenToHalfOpen: Boolean = CircuitBreakerData.DEFAULT_AUTOMATIC_TRANSITION

    /**
     * Maximum time in HALF_OPEN before an automatic state transition. Zero disables the timeout.
     * Defaults to [Duration.ZERO].
     *
     * @since 1.0.0
     */
    public var maxWaitDurationInHalfOpenState: Duration = CircuitBreakerData.DEFAULT_MAX_WAIT_IN_HALF_OPEN

    /**
     * State to transition to when [maxWaitDurationInHalfOpenState] elapses. Must be OPEN or CLOSED.
     * Defaults to [CircuitBreakerState.OPEN].
     *
     * @since 1.0.0
     */
    public var transitionStateAfterWaitDuration: CircuitBreakerState = CircuitBreakerData.DEFAULT_TRANSITION_STATE_AFTER_WAIT

    /**
     * The initial state of the circuit breaker.
     * Defaults to [CircuitBreakerState.CLOSED].
     *
     * @since 1.0.0
     */
    public var initialState: CircuitBreakerState = CircuitBreakerData.DEFAULT_INITIAL_STATE

    private var recordExceptionPredicate: ExceptionPredicate = recordAllExceptions
    private var ignoreExceptionPredicate: ExceptionPredicate = ignoreNoExceptions
    private var recordResultPredicate: ResultPredicate = recordNoResults

    /**
     * Configures which exceptions are recorded as failures using a builder DSL.
     *
     * @since 1.0.0
     */
    public fun recordException(block: ExceptionPredicateBuilder.() -> Unit) {
        recordExceptionPredicate = ExceptionPredicateBuilder().apply(block).build()
    }

    /**
     * Sets the exception-record predicate directly.
     *
     * @since 1.0.0
     */
    public fun recordException(predicate: ExceptionPredicate) {
        recordExceptionPredicate = predicate
    }

    /**
     * Configures which exceptions are ignored (not counted) using a builder DSL.
     *
     * @since 1.0.0
     */
    public fun ignoreException(block: ExceptionPredicateBuilder.() -> Unit) {
        ignoreExceptionPredicate = ExceptionPredicateBuilder().apply(block).build()
    }

    /**
     * Sets the exception-ignore predicate directly.
     *
     * @since 1.0.0
     */
    public fun ignoreException(predicate: ExceptionPredicate) {
        ignoreExceptionPredicate = predicate
    }

    /**
     * Configures a predicate that classifies successful results as failures.
     *
     * @since 1.0.0
     */
    public fun recordResult(predicate: ResultPredicate) {
        recordResultPredicate = predicate
    }

    /**
     * Sets the fixed wait duration in the OPEN state. This is a convenience shorthand for
     * setting [waitIntervalInOpenState] to [fixedWaitInterval].
     *
     * @since 1.0.0
     */
    public fun waitDurationInOpenState(duration: Duration) {
        waitIntervalInOpenState = fixedWaitInterval(duration)
    }

    override fun data(): CircuitBreakerData = CircuitBreakerData(
        eventData = eventBuilder.produce(),
        failureRateThreshold = failureRateThreshold,
        slowCallRateThreshold = slowCallRateThreshold,
        slowCallDurationThreshold = slowCallDurationThreshold,
        waitIntervalInOpenState = waitIntervalInOpenState,
        permittedCallsInHalfOpenState = permittedCallsInHalfOpenState,
        minimumNumberOfCalls = minimumNumberOfCalls,
        slidingWindowSize = slidingWindowSize,
        slidingWindowType = slidingWindowType,
        automaticTransitionFromOpenToHalfOpen = automaticTransitionFromOpenToHalfOpen,
        maxWaitDurationInHalfOpenState = maxWaitDurationInHalfOpenState,
        transitionStateAfterWaitDuration = transitionStateAfterWaitDuration,
        initialState = initialState,
        recordExceptionPredicate = recordExceptionPredicate,
        ignoreExceptionPredicate = ignoreExceptionPredicate,
        recordResultPredicate = recordResultPredicate,
    )
}
