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

import kotlin.time.Duration

/**
 * A snapshot of the current metrics tracked by a [RateLimiter].
 *
 * @since 1.0.0
 */
public data class RateLimiterMetrics(
    /** Number of permits currently available in the active window. */
    public val availablePermissions: Int,
    /** Configured maximum permits per refresh period. */
    public val limitForPeriod: Int,
    /** Duration of one refresh period. */
    public val limitRefreshPeriod: Duration,
    /** Number of coroutines currently waiting to acquire a permit. */
    public val numberOfWaitingThreads: Int,
    /** Total number of successful permit acquisitions. */
    public val numberOfSuccessfulAcquires: Long,
    /** Total number of rejected permit acquisitions. */
    public val numberOfFailedAcquires: Long,
    /** Cumulative wait time for successful acquisitions. */
    public val totalWaitTime: Duration,
)
