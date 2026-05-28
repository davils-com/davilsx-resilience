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

import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData

/**
 * A specialized [ResilienceRegistry] for storing and managing [TimeLimiter] instances.
 *
 * This registry allows for centralized management of time limiter configurations across the application.
 * By default, it provides a global instance through the companion object, but multiple instances
 * can be created for use in different contexts, such as Dependency Injection.
 *
 * @param registryData Configuration data for the registry.
 * @since 1.0.0
 */
public class TimeLimiterRegistry(
    override val registryData: ResilienceRegistryData
) : ResilienceRegistry<TimeLimiterEvent, TimeLimiterData, TimeLimiterBuilder, TimeLimiter>() {
    override fun createBuilder(): TimeLimiterBuilder = TimeLimiterBuilder()

    override fun createComponent(data: TimeLimiterData): TimeLimiter = TimeLimiter(data)
}

public fun timeLimiterRegistry(builder: ResilienceRegistryBuilder.() -> Unit): TimeLimiterRegistry {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return TimeLimiterRegistry(data)
}
