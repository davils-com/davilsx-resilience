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

import com.davils.resilience.cache.CacheStore

/**
 * A [CacheStore] that delegates all operations to the provided suspend functions.
 *
 * Use this adapter to connect a [com.davils.resilience.cache.Cache] to databases, HTTP clients,
 * or repositories without defining a custom [CacheStore] implementation class.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.0.0
 */
public class DelegatingCacheStore<K, V> internal constructor(
    private val loadDelegate: suspend (K) -> V?,
    private val storeDelegate: suspend (K, V) -> Unit,
    private val removeDelegate: suspend (K) -> Unit,
) : CacheStore<K, V> {
    override suspend fun load(key: K): V? = loadDelegate(key)

    override suspend fun store(key: K, value: V) {
        storeDelegate(key, value)
    }

    override suspend fun remove(key: K) {
        removeDelegate(key)
    }
}

/**
 * Creates a [CacheStore] backed by the provided suspend delegates.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param load Loads a value for the given key, or returns `null` if absent.
 * @param store Persists a value for the given key.
 * @param remove Removes the value for the given key.
 * @return A new [DelegatingCacheStore] instance.
 * @since 1.0.0
 */
public fun <K, V> delegatingCacheStore(
    load: suspend (K) -> V?,
    store: suspend (K, V) -> Unit,
    remove: suspend (K) -> Unit,
): DelegatingCacheStore<K, V> = DelegatingCacheStore(
    loadDelegate = load,
    storeDelegate = store,
    removeDelegate = remove,
)
