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
import kotlinx.coroutines.sync.withLock
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
    private val hitCount = atomic(0L)
    private val missCount = atomic(0L)
    private val putCount = atomic(0L)
    private val removeCount = atomic(0L)
    private val evictionCount = atomic(0L)
    private val expirationCount = atomic(0L)
    private val loadSuccessCount = atomic(0L)
    private val loadFailureCount = atomic(0L)
    private val writeSuccessCount = atomic(0L)
    private val writeFailureCount = atomic(0L)
    private val writeBackFlushedEntriesCount = atomic(0L)
    private val clearCount = atomic(0L)
    private val maintenanceJobs = mutableListOf<Job>()

    init {
        if (data.cleanupInterval > Duration.ZERO) {
            scheduleMaintenance(data.cleanupInterval) { cleanupExpired() }
        }

        if (data.writeStrategy == WriteStrategy.WRITE_BACK) {
            scheduleMaintenance(data.writeBackConfig.flushInterval) { flush() }
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
        lookupAndTouch(key)?.let { value ->
            emitHit(key)
            return value
        }

        emitMiss(key)
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
        return lookupAndTouch(key)
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
        lookupAndTouch(key)?.let { value ->
            emitHit(key)
            return value
        }

        emitMiss(key)
        loadFromStore(key)?.let { return it }

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
        emitPut(key)
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

        emitRemove(key)
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
     * Returns a snapshot of the current metrics.
     *
     * @since 1.0.0
     */
    public suspend fun getMetrics(): CacheMetrics = mutex.withLock {
        val hits = hitCount.value
        val misses = missCount.value
        val lookups = hits + misses
        CacheMetrics(
            numberOfHits = hits,
            numberOfMisses = misses,
            numberOfPuts = putCount.value,
            numberOfRemoves = removeCount.value,
            numberOfEvictions = evictionCount.value,
            numberOfExpirations = expirationCount.value,
            numberOfLoadSuccesses = loadSuccessCount.value,
            numberOfLoadFailures = loadFailureCount.value,
            numberOfWriteSuccesses = writeSuccessCount.value,
            numberOfWriteFailures = writeFailureCount.value,
            numberOfWriteBackFlushedEntries = writeBackFlushedEntriesCount.value,
            numberOfClears = clearCount.value,
            currentSize = entries.size(),
            hitRate = if (lookups == 0L) -1f else hits.toFloat() / lookups * 100f,
        )
    }

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

        emitClear()
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

        val pending = dirty.entries().associate { it.key to it.value }
        if (pending.isEmpty()) return

        var flushedCount = 0
        pending.forEach { (key, value) ->
            if (executeStoreWrite(key, rethrow = false) { it.store(key, value) }) {
                dirty.remove(key)
                flushedCount++
            }
        }

        if (flushedCount > 0) {
            writeBackFlushedEntriesCount.addAndGet(flushedCount.toLong())
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

    private suspend fun lookupAndTouch(key: K): V? {
        val entry = lookupEntry(key) ?: return null
        val touched = entry.accessed()
        entries.put(key, touched)
        return touched.value
    }

    private fun scheduleMaintenance(interval: Duration, block: suspend () -> Unit) {
        maintenanceJobs += data.eventData.scope.launch {
            while (isActive) {
                delay(interval)
                if (isDisposed()) return@launch
                block()
            }
        }
    }

    private suspend fun lookupEntry(key: K): CacheEntry<V>? {
        val entryOption = entries.get(key)
        if (entryOption is Option.None) return null

        val entry = entryOption.getOrNull() ?: return null
        if (!entry.isExpired(data.expireAfterWrite, data.expireAfterAccess)) {
            return entry
        }

        entries.remove(key)
        emitExpiration(key)
        return null
    }

    private suspend fun loadFromStore(key: K): V? {
        val store = data.store ?: return null

        return try {
            val loaded = store.load(key) ?: return null
            insert(key, loaded, persistToStore = false)
            emitLoadSuccess(key)
            loaded
        } catch (throwable: Throwable) {
            emitLoadFailure(key, throwable)
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
            emitEviction(victim)
        }
    }

    private suspend fun bufferWriteBack(key: K, value: V) {
        dirty.put(key, value)
        if (dirty.size() >= data.writeBackConfig.batchSize) {
            flush()
        }
    }

    private suspend fun persistValue(key: K, value: V) {
        executeStoreWrite(key) { it.store(key, value) }
    }

    private suspend fun persistRemoval(key: K) {
        executeStoreWrite(key) { it.remove(key) }
    }

    private suspend fun executeStoreWrite(
        key: K,
        rethrow: Boolean = true,
        operation: suspend (CacheStore<K, V>) -> Unit,
    ): Boolean {
        val store = data.store ?: return false

        return try {
            operation(store)
            emitWriteSuccess(key)
            true
        } catch (throwable: Throwable) {
            emitWriteFailure(key, throwable)
            if (rethrow) throw throwable
            false
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
                emitExpiration(key)
            }
        }
    }

    private suspend fun emitHit(key: K) {
        hitCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheHit(key))
    }

    private suspend fun emitMiss(key: K) {
        missCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheMiss(key))
    }

    private suspend fun emitPut(key: K) {
        putCount.incrementAndGet()
        eventBus.push(CacheEvent.CachePut(key))
    }

    private suspend fun emitRemove(key: K) {
        removeCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheRemove(key))
    }

    private suspend fun emitEviction(key: K) {
        evictionCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheEviction(key))
    }

    private suspend fun emitExpiration(key: K) {
        expirationCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheExpiration(key))
    }

    private suspend fun emitLoadSuccess(key: K) {
        loadSuccessCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheLoadSuccess(key))
    }

    private suspend fun emitLoadFailure(key: K, throwable: Throwable) {
        loadFailureCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheLoadFailure(key, throwable))
    }

    private suspend fun emitWriteSuccess(key: K) {
        writeSuccessCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheWriteSuccess(key))
    }

    private suspend fun emitWriteFailure(key: K, throwable: Throwable) {
        writeFailureCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheWriteFailure(key, throwable))
    }

    private suspend fun emitClear() {
        clearCount.incrementAndGet()
        eventBus.push(CacheEvent.CacheCleared)
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
