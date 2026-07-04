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
 * Built-in eviction strategy types.
 *
 * @since 1.0.0
 */
public enum class EvictionStrategyType {
    /**
     * Evicts the least recently accessed entry.
     *
     * @since 1.0.0
     */
    LRU,

    /**
     * Evicts the least frequently accessed entry.
     *
     * @since 1.0.0
     */
    LFU,

    /**
     * Evicts the oldest inserted entry.
     *
     * @since 1.0.0
     */
    FIFO,
}

/**
 * Selects a cache entry to evict when the cache exceeds its maximum size.
 *
 * Implementations must be stateless; all eviction metadata lives in [CacheEntry].
 *
 * @since 1.0.0
 */
public interface EvictionStrategy {
    /**
     * Selects the key of the entry that should be evicted.
     *
     * @param K The type of cache keys.
     * @param entries A snapshot of the current cache entries.
     * @return The key to evict, or `null` if no entry can be selected.
     * @since 1.0.0
     */
    public fun <K> selectVictim(entries: Map<K, CacheEntry<*>>): K?

    /**
     * Least Recently Used eviction strategy.
     *
     * @since 1.0.0
     */
    public data object Lru : EvictionStrategy {
        override fun <K> selectVictim(entries: Map<K, CacheEntry<*>>): K? {
            if (entries.isEmpty()) return null

            return entries.maxBy { (_, entry) -> entry.lastAccessedAt.elapsedNow() }.key
        }
    }

    /**
     * Least Frequently Used eviction strategy.
     *
     * @since 1.0.0
     */
    public data object Lfu : EvictionStrategy {
        override fun <K> selectVictim(entries: Map<K, CacheEntry<*>>): K? {
            if (entries.isEmpty()) return null

            return entries.minWith(
                compareBy<Map.Entry<K, CacheEntry<*>>> { it.value.accessCount }
                    .thenBy { it.value.insertionSeq },
            ).key
        }
    }

    /**
     * First In First Out eviction strategy.
     *
     * @since 1.0.0
     */
    public data object Fifo : EvictionStrategy {
        override fun <K> selectVictim(entries: Map<K, CacheEntry<*>>): K? {
            if (entries.isEmpty()) return null

            return entries.minBy { (_, entry) -> entry.insertionSeq }.key
        }
    }
}

/**
 * Resolves an [EvictionStrategyType] to its corresponding [EvictionStrategy] implementation.
 *
 * @return The eviction strategy for the given type.
 * @since 1.0.0
 */
public fun EvictionStrategyType.toStrategy(): EvictionStrategy = when (this) {
    EvictionStrategyType.LRU -> EvictionStrategy.Lru
    EvictionStrategyType.LFU -> EvictionStrategy.Lfu
    EvictionStrategyType.FIFO -> EvictionStrategy.Fifo
}
