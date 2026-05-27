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

import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.event.ResilienceEventData
import com.davils.resilience.retry.predicate.Predicate
import com.davils.resilience.retry.strategy.BackoffStrategy

/**
 * Data class representing the configuration for a retry operation.
 *
 * This class holds all parameters necessary to execute a retry logic, including
 * the maximum number of attempts, the backoff strategy, and the retry predicate.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class RetryData internal constructor(
    /**
     * The maximum number of attempts allowed, including the initial call.
     *
     * A value of 1 means the operation is executed once without any retry.
     *
     * @since 1.0.0
     */
    val maxAttempts: Int,

    /**
     * If true, the retry will fail after the maximum number of retries.
     * If false, the retry will continue indefinitely until a successful result is obtained.
     *
     * @since 1.0.0
     */
    val failAfterMaxRetries: Boolean,

    /**
     * The strategy used to calculate the delay between retry attempts.
     *
     * @since 1.0.0
     */
    val backoffStrategy: BackoffStrategy,

    /**
     * The condition that determines whether a retry should be attempted based on the failure.
     *
     * @since 1.0.0
     */
    val predicate: Predicate,

    /**
     * The behavior applied when a result-based retry exhausts [maxAttempts] while
     * [failAfterMaxRetries] is true.
     *
     * Defaults to [OnResultExhaustion.THROW] so that silent acceptance of values previously
     * rejected by the predicate cannot mask real failures. This setting has no effect on
     * exception-based exhaustion, which always rethrows the last caught [Throwable].
     *
     * @since 1.0.0
     */
    val onResultExhaustion: OnResultExhaustion,

    /**
     * Event data for retry events emitted by the retry operation.
     *
     * @since 1.0.0
     */
    override val eventData: ResilienceEventData
) : ResilienceComponentData {
    override fun validate(): DslVerification = verifyDsl {
        if (maxAttempts < 1) {
            fail("maxAttempts must be at least 1", "maxAttempts")
        }
    }
}
