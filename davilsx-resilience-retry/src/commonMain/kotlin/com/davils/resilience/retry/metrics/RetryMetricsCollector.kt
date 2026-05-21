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

package com.davils.resilience.retry.metrics

import com.davils.kore.pattern.event.EventBus
import com.davils.resilience.retry.event.RetryEvent
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

public class RetryMetricsCollector internal constructor(
    private val data: RetryMetricsCollectorData,
    private val eventBus: EventBus<RetryEvent>,
) {
    private val _successfulCallsWithoutRetry: AtomicLong = atomic(0L)

    public val successfulCallsWithoutRetry: Long get() = _successfulCallsWithoutRetry.value

    public fun collectMetrics() {
        if (!data.enabled) {
            return
        }

        recordAttempt()
    }

    private fun recordAttempt() {
        eventBus.subscribe<RetryEvent.RetryAttemptStarted> { _ ->
            _successfulCallsWithoutRetry.incrementAndGet()
        }
    }
}
