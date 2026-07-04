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

import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.circuitbreaker.predicate.ExceptionPredicate
import com.davils.resilience.circuitbreaker.predicate.ResultPredicate
import com.davils.resilience.circuitbreaker.strategy.WaitIntervalStrategy
import com.davils.resilience.circuitbreaker.strategy.fixedWaitInterval
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.event.ResilienceEventData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration data for a [CircuitBreaker].
 *
 * Defaults are aligned with resilience4j's defaults for drop-in familiarity.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class CircuitBreakerData internal constructor(
    override val eventData: ResilienceEventData,

    /**
     * Failure rate threshold in percent (0–100). Calls are opened when this is exceeded
     * after [minimumNumberOfCalls] have been recorded.
     *
     * @since 1.0.0
     */
    public val failureRateThreshold: Float,

    /**
     * Slow-call rate threshold in percent (0–100). Circuit opens when the ratio of calls
     * that exceed [slowCallDurationThreshold] surpasses this value.
     *
     * @since 1.0.0
     */
    public val slowCallRateThreshold: Float,

    /**
     * Duration above which a call is considered slow.
     *
     * @since 1.0.0
     */
    public val slowCallDurationThreshold: Duration,

    /**
     * Strategy controlling how long the circuit stays OPEN before moving to HALF_OPEN.
     *
     * @since 1.0.0
     */
    public val waitIntervalInOpenState: WaitIntervalStrategy,

    /**
     * Number of probe calls permitted in HALF_OPEN state.
     *
     * @since 1.0.0
     */
    public val permittedCallsInHalfOpenState: Int,

    /**
     * Minimum number of calls required before the failure/slow-call rate is evaluated.
     *
     * @since 1.0.0
     */
    public val minimumNumberOfCalls: Int,

    /**
     * Size of the sliding window. For COUNT_BASED this is the number of calls;
     * for TIME_BASED it is the number of seconds.
     *
     * @since 1.0.0
     */
    public val slidingWindowSize: Int,

    /**
     * The type of sliding window to use.
     *
     * @since 1.0.0
     */
    public val slidingWindowType: SlidingWindowType,

    /**
     * When true, the circuit breaker automatically transitions from OPEN to HALF_OPEN
     * after [waitIntervalInOpenState] without requiring a new call attempt.
     *
     * @since 1.0.0
     */
    public val automaticTransitionFromOpenToHalfOpen: Boolean,

    /**
     * Maximum time the circuit stays in HALF_OPEN before transitioning to
     * [transitionStateAfterWaitDuration]. Zero disables the timeout.
     *
     * @since 1.0.0
     */
    public val maxWaitDurationInHalfOpenState: Duration,

    /**
     * State to transition to when [maxWaitDurationInHalfOpenState] elapses.
     * Must be [CircuitBreakerState.OPEN] or [CircuitBreakerState.CLOSED].
     *
     * @since 1.0.0
     */
    public val transitionStateAfterWaitDuration: CircuitBreakerState,

    /**
     * The initial state of the circuit breaker when first created.
     *
     * @since 1.0.0
     */
    public val initialState: CircuitBreakerState,

    /**
     * Predicate that determines which exceptions are recorded as failures.
     * By default all exceptions are recorded.
     *
     * @since 1.0.0
     */
    public val recordExceptionPredicate: ExceptionPredicate,

    /**
     * Predicate that determines which exceptions are ignored (not counted at all).
     * Ignore takes precedence over record. By default no exceptions are ignored.
     *
     * @since 1.0.0
     */
    public val ignoreExceptionPredicate: ExceptionPredicate,

    /**
     * Predicate that determines whether a successful result should be treated as a failure.
     * By default no results trigger a failure.
     *
     * @since 1.0.0
     */
    public val recordResultPredicate: ResultPredicate,

) : ResilienceComponentData {
    override fun validate(): DslVerification = verifyDsl {
        if (failureRateThreshold !in 0f..100f) {
            fail("failureRateThreshold must be between 0 and 100", "failureRateThreshold")
        }
        if (slowCallRateThreshold !in 0f..100f) {
            fail("slowCallRateThreshold must be between 0 and 100", "slowCallRateThreshold")
        }
        if (slowCallDurationThreshold.isNegative()) {
            fail("slowCallDurationThreshold must be non-negative", "slowCallDurationThreshold")
        }
        if (permittedCallsInHalfOpenState < 1) {
            fail("permittedCallsInHalfOpenState must be at least 1", "permittedCallsInHalfOpenState")
        }
        if (minimumNumberOfCalls < 1) {
            fail("minimumNumberOfCalls must be at least 1", "minimumNumberOfCalls")
        }
        if (slidingWindowSize < 1) {
            fail("slidingWindowSize must be at least 1", "slidingWindowSize")
        }
        if (slidingWindowType == SlidingWindowType.COUNT_BASED && minimumNumberOfCalls > slidingWindowSize) {
            fail("minimumNumberOfCalls must not exceed slidingWindowSize for COUNT_BASED windows", "minimumNumberOfCalls")
        }
        if (maxWaitDurationInHalfOpenState.isNegative()) {
            fail("maxWaitDurationInHalfOpenState must be non-negative", "maxWaitDurationInHalfOpenState")
        }
        val validTransitionStates = setOf(CircuitBreakerState.OPEN, CircuitBreakerState.CLOSED)
        if (transitionStateAfterWaitDuration !in validTransitionStates) {
            fail(
                "transitionStateAfterWaitDuration must be OPEN or CLOSED",
                "transitionStateAfterWaitDuration"
            )
        }
    }

    public companion object {
        public val DEFAULT_FAILURE_RATE_THRESHOLD: Float = 50f
        public val DEFAULT_SLOW_CALL_RATE_THRESHOLD: Float = 100f
        public val DEFAULT_SLOW_CALL_DURATION_THRESHOLD: Duration = 60.seconds
        public val DEFAULT_WAIT_INTERVAL_IN_OPEN_STATE: WaitIntervalStrategy = fixedWaitInterval(60.seconds)
        public const val DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE: Int = 10
        public const val DEFAULT_MINIMUM_NUMBER_OF_CALLS: Int = 100
        public const val DEFAULT_SLIDING_WINDOW_SIZE: Int = 100
        public val DEFAULT_SLIDING_WINDOW_TYPE: SlidingWindowType = SlidingWindowType.COUNT_BASED
        public const val DEFAULT_AUTOMATIC_TRANSITION: Boolean = false
        public val DEFAULT_MAX_WAIT_IN_HALF_OPEN: Duration = Duration.ZERO
        public val DEFAULT_TRANSITION_STATE_AFTER_WAIT: CircuitBreakerState = CircuitBreakerState.OPEN
        public val DEFAULT_INITIAL_STATE: CircuitBreakerState = CircuitBreakerState.CLOSED
    }
}
