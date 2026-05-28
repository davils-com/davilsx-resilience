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

import com.davils.resilience.bulkhead.event.BulkheadEvent
import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData

/**
 * Registry for managing and retrieving [Bulkhead] instances.
 *
 * Ensures that bulkheads are reused and provides a central point for bulkhead management.
 * By default, it provides a global instance through the companion object, but multiple instances
 * can be created for use in different contexts, such as Dependency Injection.
 *
 * @param registryData Configuration data for the registry.
 * @since 1.0.0
 */
public class BulkheadRegistry(
    override val registryData: ResilienceRegistryData
) : ResilienceRegistry<BulkheadEvent, BulkheadData, BulkheadBuilder, Bulkhead>() {
    override fun createBuilder(): BulkheadBuilder = BulkheadBuilder()

    override fun createComponent(data: BulkheadData): Bulkhead = Bulkhead(data)
}

public fun bulkheadRegistry(builder: ResilienceRegistryBuilder.() -> Unit): BulkheadRegistry {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return BulkheadRegistry(data)
}
