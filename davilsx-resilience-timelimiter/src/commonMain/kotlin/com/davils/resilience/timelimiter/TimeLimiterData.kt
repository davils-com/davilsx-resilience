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

package com.davils.resilience.timelimiter

import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.event.ResilienceEventData
import kotlin.time.Duration

@ConsistentCopyVisibility
public data class TimeLimiterData internal constructor(
    val timeout: Duration,
    val cancelOnTimeout: Boolean,
    val strategy: TimeoutStrategy,
    val fallback: (suspend (Throwable) -> Any?)?,
    override val eventData: ResilienceEventData,
) : ResilienceComponentData {
    override fun validate(): DslVerification = verifyDsl {
        if (timeout.isNegative()) {
            fail("timeout must be non-negative", "timeout")
        }
    }
}
