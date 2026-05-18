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

package com.davils.resilience.retry.strategy

import kotlin.time.Duration

/**
 * Defines a strategy for calculating the delay between retry attempts.
 *
 * Backoff strategies are used to determine how long the system should wait before
 * retrying a failed operation. Common strategies include constant, exponential,
 * and jitter-based delays.
 *
 * @since 1.0.0
 */
public interface BackoffStrategy {
    /**
     * Calculates the delay for the given retry attempt.
     *
     * @param attempt The current retry attempt number (starting from 1).
     * @return The duration to wait before the next attempt.
     * @since 1.0.0
     */
    public fun calculateDelay(attempt: Int): Duration
}
