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

package com.davils.resilience.metrics.retry

import com.davils.resilience.metrics.MetricsCollector
import com.davils.resilience.metrics.retry.data.RetryCallMetrics
import com.davils.resilience.metrics.retry.data.RetryMetricsData
import com.davils.resilience.metrics.retry.data.RetryPerformanceMetrics
import com.davils.resilience.metrics.retry.data.RetryRealtimeMetrics
import com.davils.resilience.metrics.retry.data.RetryTryMetrics
import com.davils.resilience.retry.Retry

public class RetryMetricsCollector internal constructor(
    override val component: Retry,
) : MetricsCollector<Retry>() {
    override fun scrape() {
        // no-op — allMetrics() is suspend; async collection can be added later
    }

    public suspend fun allMetrics(): RetryMetricsData {
        val snapshot = component.getMetrics()
        val totalCalls = snapshot.totalCalls
        val attemptsPerCall = if (totalCalls == 0L) {
            0.0
        } else {
            snapshot.totalAttempts.toDouble() / totalCalls
        }

        return RetryMetricsData(
            callMetrics = RetryCallMetrics(
                totalCalls = snapshot.totalCalls,
                successfulCalls = snapshot.successfulCalls,
                exhaustedCalls = snapshot.exhaustedCalls,
                failedNonRetryableCalls = snapshot.failedNonRetryableCalls,
                canceledCalls = snapshot.canceledCalls,
            ),
            tryMetrics = RetryTryMetrics(
                totalAttempts = snapshot.totalAttempts,
                successfulAttempts = snapshot.successfulAttempts,
                failedAttempts = snapshot.failedAttempts,
                attemptsPerCall = attemptsPerCall,
            ),
            performanceMetrics = RetryPerformanceMetrics(
                callDuration = snapshot.totalCallDuration,
                attemptDuration = snapshot.totalAttemptDuration,
                backoffDuration = snapshot.totalBackoffDuration,
                totalBackoffDuration = snapshot.totalBackoffDuration,
            ),
            realtimeMetrics = RetryRealtimeMetrics(
                callsActive = snapshot.callsActive,
                callsWaiting = snapshot.callsWaiting,
            ),
        )
    }
}
