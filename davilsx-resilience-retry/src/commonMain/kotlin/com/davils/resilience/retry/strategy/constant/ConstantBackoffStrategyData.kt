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

package com.davils.resilience.retry.strategy.constant

import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import kotlin.time.Duration

/**
 * Data class containing the configuration for a constant backoff strategy.
 *
 * This class is typically instantiated via [ConstantBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ConstantBackoffStrategyData internal constructor(
    /**
     * The fixed delay duration between retry attempts.
     *
     * @since 1.0.0
     */
    val delay: Duration
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (delay.isNegative()) {
            fail("delay must be non-negative", "delay")
        }
    }
}
