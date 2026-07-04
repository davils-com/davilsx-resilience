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

/**
 * A backing store for read-through and write-through/write-back cache operations.
 *
 * Implementations may represent databases, remote services, or any slower persistence layer.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.2.0
 */
public interface CacheStore<K, V> {
    /**
     * Loads a value for the given key from the backing store.
     *
     * @param key The key to load.
     * @return The loaded value, or `null` if no value exists for the key.
     * @since 1.2.0
     */
    public suspend fun load(key: K): V?

    /**
     * Stores a value for the given key in the backing store.
     *
     * @param key The key to store.
     * @param value The value to store.
     * @since 1.2.0
     */
    public suspend fun store(key: K, value: V)

    /**
     * Removes the value for the given key from the backing store.
     *
     * @param key The key to remove.
     * @since 1.2.0
     */
    public suspend fun remove(key: K)
}
