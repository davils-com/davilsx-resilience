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
 * Aggregated data class containing all metrics for a retry component.
 *
 * This class groups various types of metrics, including call outcomes, attempt details,
 * performance measurements, and real-time status.
 *
 * @since 1.0.0
 */
public data class RetryMetricsData(
    /**
     * Metrics related to the overall operation outcomes.
     *
     * @since 1.0.0
     */
    public val callMetrics: RetryCallMetrics,

    /**
     * Metrics related to individual retry attempts.
     *
     * @since 1.0.0
     */
    public val tryMetrics: RetryTryMetrics,

    /**
     * Metrics related to performance and latency.
     *
     * @since 1.0.0
     */
    public val performanceMetrics: RetryPerformanceMetrics,

    /**
     * Metrics related to the current real-time state.
     *
     * @since 1.0.0
     */
    public val realtimeMetrics: RetryRealtimeMetrics,
)
