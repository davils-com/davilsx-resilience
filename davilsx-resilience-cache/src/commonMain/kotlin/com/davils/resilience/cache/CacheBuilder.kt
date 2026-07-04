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

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Builder for creating instances of [CacheData].
 *
 * Provides a DSL-style API for configuring cache parameters.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.2.0
 */
@KoreDsl
public class CacheBuilder<K, V> internal constructor() : ResilienceComponentBuilder<CacheData<K, V>>() {
    /**
     * The maximum number of entries the cache may hold.
     *
     * @since 1.2.0
     */
    public var maxSize: Int = 1000

    /**
     * The strategy used to select entries for eviction.
     *
     * Defaults to [EvictionStrategy.Lru].
     *
     * @since 1.2.0
     */
    public var evictionStrategy: EvictionStrategy = EvictionStrategy.Lru

    /**
     * The maximum age of an entry since creation.
     *
     * Defaults to [Duration.ZERO], which disables write-based expiration.
     *
     * @since 1.2.0
     */
    public var expireAfterWrite: Duration = Duration.ZERO

    /**
     * The maximum idle time of an entry since last access.
     *
     * Defaults to [Duration.ZERO], which disables access-based expiration.
     *
     * @since 1.2.0
     */
    public var expireAfterAccess: Duration = Duration.ZERO

    /**
     * The interval at which expired entries are actively removed.
     *
     * Defaults to [Duration.ZERO], which disables active cleanup.
     *
     * @since 1.2.0
     */
    public var cleanupInterval: Duration = Duration.ZERO

    /**
     * The optional backing store for read-through and write operations.
     *
     * @since 1.2.0
     */
    public var store: CacheStore<K, V>? = null

    /**
     * The write strategy used when persisting values.
     *
     * Defaults to [WriteStrategy.WRITE_THROUGH].
     *
     * @since 1.2.0
     */
    public var writeStrategy: WriteStrategy = WriteStrategy.WRITE_THROUGH

    private val writeBackConfigBuilder: WriteBackConfigBuilder = WriteBackConfigBuilder()

    /**
     * Sets the maximum number of entries the cache may hold.
     *
     * @param maxSize The maximum cache size.
     * @since 1.2.0
     */
    public fun maxSize(maxSize: Int) {
        this.maxSize = maxSize
    }

    /**
     * Sets the eviction strategy using a built-in strategy type.
     *
     * @param evictionStrategyType The eviction strategy type.
     * @since 1.2.0
     */
    public fun evictionStrategy(evictionStrategyType: EvictionStrategyType) {
        this.evictionStrategy = evictionStrategyType.toStrategy()
    }

    /**
     * Sets the eviction strategy.
     *
     * @param evictionStrategy The eviction strategy.
     * @since 1.2.0
     */
    public fun evictionStrategy(evictionStrategy: EvictionStrategy) {
        this.evictionStrategy = evictionStrategy
    }

    /**
     * Sets the maximum age of an entry since creation.
     *
     * @param expireAfterWrite The write expiration duration.
     * @since 1.2.0
     */
    public fun expireAfterWrite(expireAfterWrite: Duration) {
        this.expireAfterWrite = expireAfterWrite
    }

    /**
     * Sets the maximum age of an entry since creation in milliseconds.
     *
     * @param expireAfterWriteMillis The write expiration duration in milliseconds.
     * @since 1.2.0
     */
    public fun expireAfterWrite(expireAfterWriteMillis: Long) {
        this.expireAfterWrite = expireAfterWriteMillis.milliseconds
    }

    /**
     * Sets the maximum idle time of an entry since last access.
     *
     * @param expireAfterAccess The access expiration duration.
     * @since 1.2.0
     */
    public fun expireAfterAccess(expireAfterAccess: Duration) {
        this.expireAfterAccess = expireAfterAccess
    }

    /**
     * Sets the maximum idle time of an entry since last access in milliseconds.
     *
     * @param expireAfterAccessMillis The access expiration duration in milliseconds.
     * @since 1.2.0
     */
    public fun expireAfterAccess(expireAfterAccessMillis: Long) {
        this.expireAfterAccess = expireAfterAccessMillis.milliseconds
    }

    /**
     * Sets the interval at which expired entries are actively removed.
     *
     * @param cleanupInterval The cleanup interval.
     * @since 1.2.0
     */
    public fun cleanupInterval(cleanupInterval: Duration) {
        this.cleanupInterval = cleanupInterval
    }

    /**
     * Sets the interval at which expired entries are actively removed in milliseconds.
     *
     * @param cleanupIntervalMillis The cleanup interval in milliseconds.
     * @since 1.2.0
     */
    public fun cleanupInterval(cleanupIntervalMillis: Long) {
        this.cleanupInterval = cleanupIntervalMillis.milliseconds
    }

    /**
     * Sets the backing store for read-through and write operations.
     *
     * @param store The backing store.
     * @since 1.2.0
     */
    public fun store(store: CacheStore<K, V>) {
        this.store = store
    }

    /**
     * Sets the write strategy used when persisting values.
     *
     * @param writeStrategy The write strategy.
     * @since 1.2.0
     */
    public fun writeStrategy(writeStrategy: WriteStrategy) {
        this.writeStrategy = writeStrategy
    }

    /**
     * Configures write-back behavior.
     *
     * @param block A configuration block for [WriteBackConfigBuilder].
     * @since 1.2.0
     */
    public fun writeBack(block: WriteBackConfigBuilder.() -> Unit) {
        writeBackConfigBuilder.apply(block)
    }

    override fun data(): CacheData<K, V> {
        val eventData = eventBuilder.produce()
        val writeBackConfig = writeBackConfigBuilder.produce()
        return CacheData(
            eventData = eventData,
            maxSize = maxSize,
            evictionStrategy = evictionStrategy,
            expireAfterWrite = expireAfterWrite,
            expireAfterAccess = expireAfterAccess,
            store = store,
            writeStrategy = writeStrategy,
            writeBackConfig = writeBackConfig,
            cleanupInterval = cleanupInterval,
        )
    }
}
