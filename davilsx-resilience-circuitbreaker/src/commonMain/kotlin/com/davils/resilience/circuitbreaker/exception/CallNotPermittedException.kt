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

package com.davils.resilience.circuitbreaker.exception

import com.davils.resilience.circuitbreaker.CircuitBreakerState

/**
 * Thrown when a call is rejected because the [com.davils.resilience.circuitbreaker.CircuitBreaker] is in
 * [CircuitBreakerState.OPEN] or [CircuitBreakerState.FORCED_OPEN] state.
 *
 * @param state The state of the circuit breaker at the time of rejection.
 * @since 1.0.0
 */
public class CallNotPermittedException(
    public val state: CircuitBreakerState,
    message: String = "CircuitBreaker is $state — call not permitted"
) : Exception(message)
