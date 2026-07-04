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

/**
 * A snapshot of the current metrics tracked by a [CircuitBreaker].
 *
 * All values reflect the state of the sliding window at the moment the snapshot was taken.
 *
 * @since 1.0.0
 */
public data class CircuitBreakerMetrics(
    /** Number of calls recorded in the current sliding window. */
    public val numberOfBufferedCalls: Int,
    /** Number of successful calls in the current sliding window. */
    public val numberOfSuccessfulCalls: Int,
    /** Number of failed calls in the current sliding window. */
    public val numberOfFailedCalls: Int,
    /** Number of slow calls (success + failure) in the current sliding window. */
    public val numberOfSlowCalls: Int,
    /** Number of slow successful calls in the current sliding window. */
    public val numberOfSlowSuccessfulCalls: Int,
    /** Number of slow failed calls in the current sliding window. */
    public val numberOfSlowFailedCalls: Int,
    /** Number of calls rejected because the circuit was OPEN or FORCED_OPEN. */
    public val numberOfNotPermittedCalls: Long,
    /** Failure rate as a percentage (0–100), or -1 if below [CircuitBreakerData.minimumNumberOfCalls]. */
    public val failureRate: Float,
    /** Slow-call rate as a percentage (0–100), or -1 if below [CircuitBreakerData.minimumNumberOfCalls]. */
    public val slowCallRate: Float,
)
