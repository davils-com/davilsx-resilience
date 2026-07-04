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

package com.davils.resilience.cache.store

import com.davils.kore.collections.ConcurrentHashMap
import com.davils.kore.collections.concurrentHashMapOf
import com.davils.resilience.cache.CacheStore
import kotlinx.atomicfu.atomic

/**
 * A thread-safe in-memory [CacheStore] backed by kore's [ConcurrentHashMap].
 *
 * Suitable for development, testing, and local persistence simulation behind a [com.davils.resilience.cache.Cache].
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.2.0
 */
public class InMemoryCacheStore<K, V> internal constructor(
    initial: Map<K, V> = emptyMap(),
) : CacheStore<K, V> {
    private val backing: ConcurrentHashMap<K, V> = concurrentHashMapOf()
    private val initialEntries: Map<K, V> = initial.toMap()
    private val seeded = atomic(false)

    override suspend fun load(key: K): V? {
        ensureSeeded()
        return backing.get(key).getOrNull()
    }

    override suspend fun store(key: K, value: V) {
        ensureSeeded()
        backing.put(key, value)
    }

    override suspend fun remove(key: K) {
        ensureSeeded()
        backing.remove(key)
    }

    /**
     * Returns a snapshot of all entries currently held by this store.
     *
     * @return A map containing all key-value pairs.
     * @since 1.2.0
     */
    public suspend fun snapshot(): Map<K, V> {
        ensureSeeded()
        return backing.entries().associate { it.key to it.value }
    }

    private suspend fun ensureSeeded() {
        if (!seeded.compareAndSet(expect = false, update = true)) return
        if (initialEntries.isNotEmpty()) {
            backing.putAll(initialEntries)
        }
    }
}

/**
 * Creates a thread-safe in-memory [CacheStore] optionally seeded with [initial] entries.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param initial Optional initial entries copied into the store on first access.
 * @return A new [InMemoryCacheStore] instance.
 * @since 1.2.0
 */
public fun <K, V> inMemoryCacheStore(initial: Map<K, V> = emptyMap()): InMemoryCacheStore<K, V> =
    InMemoryCacheStore(initial)
