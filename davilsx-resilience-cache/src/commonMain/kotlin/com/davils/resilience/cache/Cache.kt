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

import com.davils.kore.collections.ConcurrentHashMap
import com.davils.kore.collections.concurrentHashMapOf
import com.davils.kore.pattern.functional.Option
import com.davils.resilience.common.ResilienceComponent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * A thread-safe cache resilience component with configurable eviction, TTL, and write strategies.
 *
 * The [Cache] stores entries in a [ConcurrentHashMap] and optionally delegates read and write
 * operations to a [CacheStore]. It supports LRU, LFU, and FIFO eviction, lazy and active TTL
 * expiration, write-through and write-back persistence, and emits [CacheEvent] instances for
 * observability.
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.0.0
 */
public class Cache<K, V> internal constructor(
    override val data: CacheData<K, V>,
) : ResilienceComponent<CacheData<K, V>, CacheEvent>() {
    override val disposeEvent: CacheEvent
        get() = CacheEvent.CacheDispose

    private val entries: ConcurrentHashMap<K, CacheEntry<V>> = concurrentHashMapOf()
    private val dirty: ConcurrentHashMap<K, V> = concurrentHashMapOf()
    private val insertionSeq = atomic(0L)
    private val maintenanceJobs = mutableListOf<Job>()

    init {
        if (data.cleanupInterval > Duration.ZERO) {
            maintenanceJobs += data.eventData.scope.launch {
                while (isActive) {
                    delay(data.cleanupInterval)
                    if (isDisposed()) return@launch
                    cleanupExpired()
                }
            }
        }

        if (data.writeStrategy == WriteStrategy.WRITE_BACK) {
            maintenanceJobs += data.eventData.scope.launch {
                while (isActive) {
                    delay(data.writeBackConfig.flushInterval)
                    if (isDisposed()) return@launch
                    flush()
                }
            }
        }
    }

    /**
     * Returns the value associated with the given [key], or `null` if no valid entry exists.
     *
     * This method emits [CacheEvent.CacheHit] and [CacheEvent.CacheMiss] events. If a backing
     * [CacheStore] is configured, a cache miss triggers a read-through load outside the map lock.
     * Concurrent misses for the same key may load more than once; the last write wins.
     *
     * @param key The key to look up.
     * @return The cached value, or `null` if no valid entry exists.
     * @since 1.0.0
     */
    public suspend fun get(key: K): V? {
        checkDisposal()
        lookupEntry(key)?.let { entry ->
            entries.computeIfPresent(key) { _, current -> Option.some(current.accessed()) }
            eventBus.push(CacheEvent.CacheHit(key))
            return entry.value
        }

        eventBus.push(CacheEvent.CacheMiss(key))
        return loadFromStore(key)
    }

    /**
     * Returns the value associated with the given [key], or `null` if no valid entry exists.
     *
     * Unlike [get], this method does not emit cache events.
     *
     * @param key The key to look up.
     * @return The cached value, or `null` if no valid entry exists.
     * @since 1.0.0
     */
    public suspend fun getOrNull(key: K): V? {
        checkDisposal()
        lookupEntry(key)?.let { entry ->
            entries.computeIfPresent(key) { _, current -> Option.some(current.accessed()) }
            return entry.value
        }
        return null
    }

    /**
     * Returns the value associated with the given [key], loading it with [loader] on a cache miss.
     *
     * @param key The key to look up.
     * @param loader The suspendable loader invoked when the key is not present.
     * @return The cached or loaded value.
     * @since 1.0.0
     */
    public suspend fun get(key: K, loader: suspend (K) -> V): V {
        checkDisposal()
        lookupEntry(key)?.let { entry ->
            entries.computeIfPresent(key) { _, current -> Option.some(current.accessed()) }
            eventBus.push(CacheEvent.CacheHit(key))
            return entry.value
        }

        eventBus.push(CacheEvent.CacheMiss(key))
        loadFromStore(key)?.let { loaded ->
            return loaded
        }

        val value = loader(key)
        put(key, value)
        return value
    }

    /**
     * Stores the given [value] under [key].
     *
     * @param key The key to store.
     * @param value The value to store.
     * @since 1.0.0
     */
    public suspend fun put(key: K, value: V) {
        checkDisposal()
        insert(key, value, persistToStore = true)
        eventBus.push(CacheEvent.CachePut(key))
    }

    /**
     * Removes the entry associated with the given [key].
     *
     * @param key The key to remove.
     * @since 1.0.0
     */
    public suspend fun remove(key: K) {
        checkDisposal()
        entries.remove(key)
        dirty.remove(key)

        if (shouldMirrorRemovalsToStore()) {
            persistRemoval(key)
        }

        eventBus.push(CacheEvent.CacheRemove(key))
    }

    /**
     * Returns `true` if a valid, non-expired entry exists for the given [key].
     *
     * @param key The key to check.
     * @return `true` if a valid entry exists, `false` otherwise.
     * @since 1.0.0
     */
    public suspend fun contains(key: K): Boolean {
        checkDisposal()
        return lookupEntry(key) != null
    }

    /**
     * Returns the number of entries currently held by the cache.
     *
     * Expired entries are included until they are lazily removed or cleaned up actively.
     *
     * @return The current number of entries.
     * @since 1.0.0
     */
    public suspend fun size(): Long = entries.size()

    /**
     * Returns a snapshot of all keys currently held by the cache.
     *
     * @return A set of cache keys.
     * @since 1.0.0
     */
    public suspend fun keys(): Set<K> = entries.keys()

    /**
     * Removes all entries from the cache.
     *
     * @since 1.0.0
     */
    public suspend fun clear() {
        checkDisposal()
        val keysSnapshot = entries.keys()

        entries.clear()
        dirty.clear()

        if (shouldMirrorRemovalsToStore()) {
            keysSnapshot.forEach { key ->
                persistRemoval(key)
            }
        }

        eventBus.push(CacheEvent.CacheCleared)
    }

    /**
     * Flushes buffered write-back entries to the backing store.
     *
     * Has no effect when [WriteStrategy.WRITE_THROUGH] is configured.
     *
     * @since 1.0.0
     */
    public suspend fun flush() {
        checkDisposal()
        if (data.writeStrategy != WriteStrategy.WRITE_BACK) return

        val store = data.store ?: return
        val pending = dirty.entries().associate { it.key to it.value }
        if (pending.isEmpty()) return

        var flushedCount = 0
        pending.forEach { (key, value) ->
            try {
                store.store(key, value)
                dirty.remove(key)
                flushedCount++
                eventBus.push(CacheEvent.CacheWriteSuccess(key))
            } catch (throwable: Throwable) {
                eventBus.push(CacheEvent.CacheWriteFailure(key, throwable))
            }
        }

        if (flushedCount > 0) {
            eventBus.push(CacheEvent.CacheWriteBackFlushed(flushedCount))
        }
    }

    /**
     * Disposes of the cache and its resources.
     *
     * Cancels maintenance coroutines, optionally flushes write-back entries, and closes the event bus.
     *
     * @since 1.0.0
     */
    override suspend fun dispose() {
        maintenanceJobs.forEach { it.cancel() }
        maintenanceJobs.clear()

        if (data.writeStrategy == WriteStrategy.WRITE_BACK && data.writeBackConfig.flushOnDispose) {
            flush()
        }

        entries.clear()
        dirty.clear()
        super.dispose()
    }

    private suspend fun lookupEntry(key: K): CacheEntry<V>? {
        val entryOption = entries.get(key)
        if (entryOption is Option.None) return null

        val entry = entryOption.getOrNull() ?: return null
        if (!entry.isExpired(data.expireAfterWrite, data.expireAfterAccess)) {
            return entry
        }

        entries.remove(key)
        eventBus.push(CacheEvent.CacheExpiration(key))
        return null
    }

    private suspend fun loadFromStore(key: K): V? {
        val store = data.store ?: return null

        return try {
            val loaded = store.load(key) ?: return null
            insert(key, loaded, persistToStore = false)
            eventBus.push(CacheEvent.CacheLoadSuccess(key))
            loaded
        } catch (throwable: Throwable) {
            eventBus.push(CacheEvent.CacheLoadFailure(key, throwable))
            null
        }
    }

    private suspend fun insert(key: K, value: V, persistToStore: Boolean) {
        val entry = CacheEntry.create(
            value = value,
            insertionSeq = insertionSeq.incrementAndGet(),
        )
        entries.put(key, entry)
        enforceMaxSize()

        if (!persistToStore) return

        when (data.writeStrategy) {
            WriteStrategy.WRITE_THROUGH -> persistValue(key, value)
            WriteStrategy.WRITE_BACK -> bufferWriteBack(key, value)
        }
    }

    private suspend fun enforceMaxSize() {
        while (entries.size() > data.maxSize) {
            val snapshot = entries.entries().associate { it.key to it.value }
            val victim = data.evictionStrategy.selectVictim(snapshot) ?: break
            entries.remove(victim)
            dirty.remove(victim)
            eventBus.push(CacheEvent.CacheEviction(victim))
        }
    }

    private suspend fun bufferWriteBack(key: K, value: V) {
        dirty.put(key, value)
        if (dirty.size() >= data.writeBackConfig.batchSize) {
            flush()
        }
    }

    private suspend fun persistValue(key: K, value: V) {
        val store = data.store ?: return

        try {
            store.store(key, value)
            eventBus.push(CacheEvent.CacheWriteSuccess(key))
        } catch (throwable: Throwable) {
            eventBus.push(CacheEvent.CacheWriteFailure(key, throwable))
            throw throwable
        }
    }

    private suspend fun persistRemoval(key: K) {
        val store = data.store ?: return

        try {
            store.remove(key)
            eventBus.push(CacheEvent.CacheWriteSuccess(key))
        } catch (throwable: Throwable) {
            eventBus.push(CacheEvent.CacheWriteFailure(key, throwable))
            throw throwable
        }
    }

    private fun shouldMirrorRemovalsToStore(): Boolean {
        return data.store != null && data.writeStrategy == WriteStrategy.WRITE_THROUGH
    }

    private suspend fun cleanupExpired() {
        val snapshot = entries.entries()
        snapshot.forEach { (key, entry) ->
            if (entry.isExpired(data.expireAfterWrite, data.expireAfterAccess)) {
                entries.remove(key)
                dirty.remove(key)
                eventBus.push(CacheEvent.CacheExpiration(key))
            }
        }
    }

    private suspend fun checkDisposal() {
        if (isDisposed()) {
            throw CancellationException("Cache instance is disposed")
        }
    }
}

/**
 * Creates a new [Cache] instance using the provided configuration [builder].
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @param builder The configuration builder block.
 * @return A new [Cache] instance.
 * @since 1.0.0
 */
public fun <K, V> cache(builder: CacheBuilder<K, V>.() -> Unit): Cache<K, V> {
    val cacheBuilder = CacheBuilder<K, V>()
    cacheBuilder.apply(builder)
    val cacheData = cacheBuilder.produce()
    return Cache(cacheData)
}
