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

import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData
import com.davils.resilience.retry.event.RetryEvent

/**
 * A specialized [ResilienceRegistry] for storing and managing [Retry] instances.
 *
 * This registry allows for centralized management of retry configurations across the application.
 * Create instances with [retryRegistry] and share them through dependency injection or application scope.
 *
 * @param registryData Configuration data for the registry.
 * @since 1.0.0
 */
public class RetryRegistry(
    override val registryData: ResilienceRegistryData
) : ResilienceRegistry<RetryEvent, RetryData, RetryBuilder, Retry>() {
    override fun createBuilder(): RetryBuilder = RetryBuilder()

    override fun createComponent(data: RetryData): Retry = Retry(data)
}

public fun retryRegistry(builder: ResilienceRegistryBuilder.() -> Unit): RetryRegistry {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return RetryRegistry(data)
}
