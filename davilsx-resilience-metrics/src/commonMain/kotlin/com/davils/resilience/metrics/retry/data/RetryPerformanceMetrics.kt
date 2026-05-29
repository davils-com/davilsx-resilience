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

package com.davils.resilience.metrics.retry.data

import kotlin.time.Duration

/**
 * Data class representing performance and latency metrics for the retry component.
 *
 * These metrics provide insights into the time overhead introduced by the retry mechanism
 * and the execution time of the protected operations.
 *
 * @since 1.0.0
 */
public data class RetryPerformanceMetrics(
    /**
     * The total duration of calls, from the start of the first attempt until completion.
     *
     * This includes the execution time of all attempts and all backoff wait periods.
     *
     * @since 1.0.0
     */
    public val callDuration: Duration,

    /**
     * The execution duration of individual attempts.
     *
     * This measures the time spent executing the protected block, excluding any backoff periods.
     *
     * @since 1.0.0
     */
    public val attemptDuration: Duration,

    /**
     * The duration of individual backoff periods between attempts.
     *
     * @since 1.0.0
     */
    public val backoffDuration: Duration,

    /**
     * The cumulative time spent waiting in backoff periods across all calls.
     *
     * @since 1.0.0
     */
    public val totalBackoffDuration: Duration,
)
