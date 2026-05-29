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

package com.davils.resilience.bulkhead

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Builder for creating instances of [BulkheadData].
 *
 * Provides a DSL-style API for configuring bulkhead parameters.
 *
 * @since 1.0.0
 */
@KoreDsl
public class BulkheadBuilder internal constructor() : ResilienceComponentBuilder<BulkheadData>() {
    /**
     * The maximum number of concurrent calls allowed by the bulkhead.
     *
     * @since 1.0.0
     */
    public var maxConcurrentCalls: Int = 25

    /**
     * The maximum duration to wait for a permit before failing.
     *
     * @since 1.0.0
     */
    public var maxWaitDuration: Duration = 500.milliseconds

    /**
     * Sets the maximum number of concurrent calls.
     *
     * @param maxConcurrentCalls The maximum number of concurrent calls.
     * @since 1.0.0
     */
    public fun maxConcurrentCalls(maxConcurrentCalls: Int) {
        this.maxConcurrentCalls = maxConcurrentCalls
    }

    /**
     * Sets the maximum duration to wait for a permit.
     *
     * @param maxWaitDuration The maximum wait duration.
     * @since 1.0.0
     */
    public fun maxWaitDuration(maxWaitDuration: Duration) {
        this.maxWaitDuration = maxWaitDuration
    }

    /**
     * Sets the maximum duration to wait for a permit in milliseconds.
     *
     * @param maxWaitDurationMillis The maximum wait duration in milliseconds.
     * @since 1.0.0
     */
    public fun maxWaitDuration(maxWaitDurationMillis: Long) {
        this.maxWaitDuration = maxWaitDurationMillis.milliseconds
    }

    /**
     * Produces a [BulkheadData] instance based on the current configuration.
     *
     * @return A configured [BulkheadData] instance.
     * @since 1.0.0
     */
    override fun data(): BulkheadData {
        val eventData = eventBuilder.produce()
        return BulkheadData(
            eventData = eventData,
            maxConcurrentCalls = maxConcurrentCalls,
            maxWaitDuration = maxWaitDuration
        )
    }
}
