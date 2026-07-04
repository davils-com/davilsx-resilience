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

import kotlin.time.Duration

/**
 * Creates a bounded cache that evicts the least recently used entry when [maxSize] is exceeded.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param maxSize The maximum number of entries before eviction runs.
 * @param builder An optional configuration block applied after the preset.
 * @return A new [Cache] instance with LRU eviction.
 * @since 1.0.0
 */
public fun <K, V> lruCache(
    maxSize: Int = 1000,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    maxSize(maxSize)
    evictionStrategy(EvictionStrategyType.LRU)
    builder()
}

/**
 * Creates a bounded cache that evicts the least frequently used entry when [maxSize] is exceeded.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param maxSize The maximum number of entries before eviction runs.
 * @param builder An optional configuration block applied after the preset.
 * @return A new [Cache] instance with LFU eviction.
 * @since 1.0.0
 */
public fun <K, V> lfuCache(
    maxSize: Int = 1000,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    maxSize(maxSize)
    evictionStrategy(EvictionStrategyType.LFU)
    builder()
}

/**
 * Creates a bounded cache that evicts the earliest inserted entry when [maxSize] is exceeded.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param maxSize The maximum number of entries before eviction runs.
 * @param builder An optional configuration block applied after the preset.
 * @return A new [Cache] instance with FIFO eviction.
 * @since 1.0.0
 */
public fun <K, V> fifoCache(
    maxSize: Int = 1000,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    maxSize(maxSize)
    evictionStrategy(EvictionStrategyType.FIFO)
    builder()
}

/**
 * Creates a pure in-memory cache without a backing [CacheStore].
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param builder An optional configuration block.
 * @return A new in-memory [Cache] instance.
 * @since 1.0.0
 */
public fun <K, V> inMemoryCache(
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache(builder)

/**
 * Creates a cache with TTL expiration configured.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param expireAfterWrite The maximum age of an entry since creation.
 * @param expireAfterAccess The maximum idle time since last access; [Duration.ZERO] disables.
 * @param cleanupInterval The interval for active expiry cleanup; [Duration.ZERO] disables.
 * @param builder An optional configuration block applied after the preset.
 * @return A new [Cache] instance with expiration enabled.
 * @since 1.0.0
 */
public fun <K, V> expiringCache(
    expireAfterWrite: Duration,
    expireAfterAccess: Duration = Duration.ZERO,
    cleanupInterval: Duration = Duration.ZERO,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    expireAfterWrite(expireAfterWrite)
    expireAfterAccess(expireAfterAccess)
    cleanupInterval(cleanupInterval)
    builder()
}

/**
 * Creates a cache backed by [store] with synchronous write-through persistence.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param store The backing store for read-through and write-through operations.
 * @param builder An optional configuration block applied after the preset.
 * @return A new write-through [Cache] instance.
 * @since 1.0.0
 */
public fun <K, V> writeThroughCache(
    store: CacheStore<K, V>,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    store(store)
    writeStrategy(WriteStrategy.WRITE_THROUGH)
    builder()
}

/**
 * Creates a cache backed by [store] with buffered write-back persistence.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param store The backing store for read-through and write-back operations.
 * @param builder An optional configuration block applied after the preset; use [CacheBuilder.writeBack]
 *   to configure flush behavior.
 * @return A new write-back [Cache] instance.
 * @since 1.0.0
 */
public fun <K, V> writeBackCache(
    store: CacheStore<K, V>,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V> = cache {
    store(store)
    writeStrategy(WriteStrategy.WRITE_BACK)
    builder()
}
