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

package com.davils.resilience.common.registry

import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.common.event.ResilienceEventData

/**
 * Data class representing the configuration for a [ResilienceRegistry].
 *
 * This class encapsulates settings such as event bus configuration
 * that govern the behavior of the registry.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ResilienceRegistryData internal constructor(
    /**
     * Configuration for the registry's event bus.
     *
     * @since 1.0.0
     */
    public val eventData: ResilienceEventData
) : DslVerifiableData {

    /**
     * Validates the registry data.
     *
     * @return The verification result.
     * @since 1.0.0
     */
    override fun validate(): DslVerification = verifyDsl {
        // eventData validation is performed within ResilienceEventData itself
    }
}
