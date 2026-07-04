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
 * The states a [CircuitBreaker] can occupy.
 *
 * The normal lifecycle is CLOSED → OPEN → HALF_OPEN → CLOSED. Manual transitions
 * to DISABLED, FORCED_OPEN and METRICS_ONLY are also supported.
 *
 * @since 1.0.0
 */
public enum class CircuitBreakerState {
    /**
     * All calls are allowed. Metrics are recorded. Transitions to [OPEN] when thresholds are exceeded.
     *
     * @since 1.0.0
     */
    CLOSED,

    /**
     * All calls are rejected. Transitions to [HALF_OPEN] after the configured wait duration has elapsed.
     *
     * @since 1.0.0
     */
    OPEN,

    /**
     * A limited number of probe calls are allowed. Transitions back to [CLOSED] if below thresholds,
     * or to [OPEN] if thresholds are still exceeded.
     *
     * @since 1.0.0
     */
    HALF_OPEN,

    /**
     * All calls are allowed and no metrics are collected. Used to temporarily disable the circuit breaker.
     *
     * @since 1.0.0
     */
    DISABLED,

    /**
     * All calls are rejected regardless of metrics. Used to manually open the circuit breaker.
     *
     * @since 1.0.0
     */
    FORCED_OPEN,

    /**
     * All calls are allowed, and metrics are collected, but thresholds are never enforced.
     * Useful for monitoring without protection.
     *
     * @since 1.0.0
     */
    METRICS_ONLY
}
