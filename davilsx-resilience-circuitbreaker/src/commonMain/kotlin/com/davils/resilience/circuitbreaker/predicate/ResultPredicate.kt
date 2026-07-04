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

package com.davils.resilience.circuitbreaker.predicate

/**
 * Determines whether a successful call result should be treated as a failure by the
 * [com.davils.resilience.circuitbreaker.CircuitBreaker].
 *
 * When the predicate returns `true` for a result, the circuit breaker wraps it in a
 * [com.davils.resilience.circuitbreaker.exception.ResultRecordedAsFailureException] and counts
 * the call as a failure in the sliding window.
 *
 * @since 1.0.0
 */
public fun interface ResultPredicate {
    /**
     * Returns `true` if [result] should be classified as a failure.
     *
     * @since 1.0.0
     */
    public fun test(result: Any?): Boolean
}

/** Default predicate that never treats a result as a failure. */
internal val recordNoResults: ResultPredicate = ResultPredicate { false }
