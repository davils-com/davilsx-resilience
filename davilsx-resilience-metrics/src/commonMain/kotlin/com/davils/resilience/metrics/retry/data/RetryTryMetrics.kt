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

/**
 * Data class representing the metrics related to individual retry attempts.
 *
 * These metrics track each attempt made by the retry component, regardless of whether
 * it belongs to the same call.
 *
 * @since 1.0.0
 */
public data class RetryTryMetrics(
    /**
     * The total number of attempts made across all calls.
     *
     * This is the absolute sum of all attempts, including initial calls and retries.
     *
     * @since 1.0.0
     */
    public val totalAttempts: Long,

    /**
     * The number of individual attempts that were successful.
     *
     * @since 1.0.0
     */
    public val successfulAttempts: Long,

    /**
     * The number of individual attempts that failed.
     *
     * This count includes every failed attempt, even if a subsequent retry was performed.
     *
     * @since 1.0.0
     */
    public val failedAttempts: Long,

    /**
     * The average number of attempts required per call.
     *
     * This value provides insight into the stability of the system. A high average
     * indicates that operations frequently require multiple retries to succeed.
     *
     * @since 1.0.0
     */
    public val attemptsPerCall: Double,
)
