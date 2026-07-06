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

package com.davils.resilience.retry

import kotlin.time.Duration

/**
 * A snapshot of the current metrics tracked by a [Retry] instance.
 *
 * @since 1.0.0
 */
public data class RetryMetrics(
    /** Total number of [Retry.execute] invocations. */
    public val totalCalls: Long,
    /** Operations that completed successfully. */
    public val successfulCalls: Long,
    /** Operations that exhausted all retry attempts. */
    public val exhaustedCalls: Long,
    /** Operations that failed without retrying (predicate rejected the failure). */
    public val failedNonRetryableCalls: Long,
    /** Operations cancelled via [kotlinx.coroutines.CancellationException]. */
    public val canceledCalls: Long,
    /** Total attempts across all calls (initial attempts plus retries). */
    public val totalAttempts: Long,
    /** Individual attempts that completed without throwing. */
    public val successfulAttempts: Long,
    /** Individual attempts that failed with an exception. */
    public val failedAttempts: Long,
    /** Cumulative time spent executing protected blocks. */
    public val totalAttemptDuration: Duration,
    /** Cumulative time spent in backoff delays. */
    public val totalBackoffDuration: Duration,
    /** Cumulative end-to-end call duration including backoff. */
    public val totalCallDuration: Duration,
    /** Calls currently executing inside [Retry.execute]. */
    public val callsActive: Long,
    /** Calls currently waiting in a backoff delay. */
    public val callsWaiting: Long,
)
