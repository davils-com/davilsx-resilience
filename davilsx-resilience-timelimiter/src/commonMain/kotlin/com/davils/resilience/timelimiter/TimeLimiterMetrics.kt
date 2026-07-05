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

package com.davils.resilience.timelimiter

import kotlin.time.Duration

/**
 * A snapshot of the current metrics tracked by a [TimeLimiter].
 *
 * @since 1.0.0
 */
public data class TimeLimiterMetrics(
    /** Configured timeout duration at the time of the snapshot. */
    public val timeout: Duration,
    /** Configured timeout strategy. */
    public val strategy: TimeoutStrategy,
    /** Whether running work is cancelled on timeout (soft strategy). */
    public val cancelOnTimeout: Boolean,
    /** Total number of successful executions. */
    public val numberOfSuccessfulCalls: Long,
    /** Total number of timed-out executions. */
    public val numberOfTimeoutCalls: Long,
    /** Cumulative execution time for successful calls. */
    public val totalExecutionTime: Duration,
)
