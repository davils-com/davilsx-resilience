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

package com.davils.resilience.cache

import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData

/**
 * Registry for managing and retrieving [Cache] instances.
 *
 * Ensures that caches are reused and provides a central point for cache management.
 * By default, it provides a global instance through the companion object, but multiple instances
 * can be created for use in different contexts, such as Dependency Injection.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param registryData Configuration data for the registry.
 * @since 1.2.0
 */
public class CacheRegistry<K, V>(
    override val registryData: ResilienceRegistryData,
) : ResilienceRegistry<CacheEvent, CacheData<K, V>, CacheBuilder<K, V>, Cache<K, V>>() {
    override fun createBuilder(): CacheBuilder<K, V> = CacheBuilder()

    override fun createComponent(data: CacheData<K, V>): Cache<K, V> = Cache(data)
}

/**
 * Creates a new [CacheRegistry] instance using the provided configuration [builder].
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param builder The configuration builder block.
 * @return A new [CacheRegistry] instance.
 * @since 1.2.0
 */
public fun <K, V> cacheRegistry(builder: ResilienceRegistryBuilder.() -> Unit): CacheRegistry<K, V> {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return CacheRegistry(data)
}
