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

package com.davils.resilience.ratelimiter

/**
 * Defines how the rate limiter behaves when a permit is not immediately available.
 *
 * @since 1.0.0
 */
public enum class RateLimiterStrategy {
    /** Rejects the request when any wait time would be required. */
    FAIL_FAST,

    /** Waits up to [RateLimiterData.timeoutDuration] before rejecting. */
    WAIT,

    /** Waits indefinitely until a permit becomes available. */
    BLOCKING,
}

/**
 * Defines the algorithm used to enforce the rate limit.
 *
 * @since 1.0.0
 */
public enum class RateLimiterWindowType {
    /** Resets the permit count at fixed interval boundaries. */
    FIXED,

    /** Counts permits acquired within a rolling time window. */
    SLIDING,
}
