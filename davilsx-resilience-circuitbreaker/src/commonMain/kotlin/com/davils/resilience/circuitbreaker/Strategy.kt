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
 * The type of sliding window used by a [CircuitBreaker] to aggregate call outcomes.
 *
 * @since 1.0.0
 */
public enum class SlidingWindowType {
    /**
     * Uses a fixed-size ring buffer. The window aggregates the last [CircuitBreakerData.slidingWindowSize] calls.
     *
     * @since 1.0.0
     */
    COUNT_BASED,

    /**
     * Uses a time-based window. The window aggregates calls recorded within the last
     * [CircuitBreakerData.slidingWindowSize] seconds.
     *
     * @since 1.0.0
     */
    TIME_BASED
}
