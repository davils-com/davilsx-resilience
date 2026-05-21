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

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl

public data class RetryMetricsCollectorData(
    public val enabled: Boolean,
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        // No validation needed, all fields not validatable
    }
}
