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

import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.event.ResilienceEventData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Configuration data for a [Bulkhead].
 *
 * This class holds the configuration parameters that define the behavior of a bulkhead,
 * such as the maximum number of concurrent calls and the maximum wait duration for a permit.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class BulkheadData internal constructor(
    /**
     * The event configuration for the bulkhead.
     *
     * @since 1.0.0
     */
    override val eventData: ResilienceEventData
): ResilienceComponentData {
    /**
     * The maximum number of concurrent calls allowed by the bulkhead.
     *
     * Must be at least 1. Defaults to 25.
     *
     * @since 1.0.0
     */
    public var maxConcurrentCalls: Int = 25

    /**
     * The maximum duration to wait for a permit before failing.
     *
     * Must be non-negative. Defaults to 500 milliseconds.
     *
     * @since 1.0.0
     */
    public var maxWaitDuration: Duration = 500.milliseconds

    /**
     * Validates the bulkhead configuration.
     *
     * Ensures that [maxConcurrentCalls] is at least 1 and [maxWaitDuration] is non-negative.
     *
     * @return A [DslVerification] object containing any validation errors.
     * @since 1.0.0
     */
    override fun validate(): DslVerification = verifyDsl {
        if (maxConcurrentCalls < 1) {
            fail("maxConcurrentCalls must be at least 1", "maxConcurrentCalls")
        }

        if (maxWaitDuration.isNegative()) {
            fail("maxWaitDuration must be non-negative", "maxWaitDuration")
        }
    }
}
