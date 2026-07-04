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

package com.davils.resilience.circuitbreaker

import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData

/**
 * Registry for managing named [CircuitBreaker] instances.
 *
 * Ensures that circuit breakers are reused and provides a central point for lifecycle management.
 *
 * @param registryData Configuration data for the registry.
 * @since 1.0.0
 */
public class CircuitBreakerRegistry(
    override val registryData: ResilienceRegistryData
) : ResilienceRegistry<CircuitBreakerEvent, CircuitBreakerData, CircuitBreakerBuilder, CircuitBreaker>() {
    override fun createBuilder(): CircuitBreakerBuilder = CircuitBreakerBuilder()

    override fun createComponent(data: CircuitBreakerData): CircuitBreaker = CircuitBreaker(data)
}

/**
 * Creates a new [CircuitBreakerRegistry] configured by [builder].
 *
 * @param builder Configuration block applied to a [ResilienceRegistryBuilder].
 * @return A configured [CircuitBreakerRegistry].
 * @since 1.0.0
 */
public fun circuitBreakerRegistry(builder: ResilienceRegistryBuilder.() -> Unit): CircuitBreakerRegistry {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return CircuitBreakerRegistry(data)
}
