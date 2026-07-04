package com.davils.resilience.cache

import kotlinx.atomicfu.atomic

/**
 * A simple in-memory [CacheStore] used for testing read/write-through and write-back behavior.
 */
internal class FakeCacheStore<K, V>(
    val backing: MutableMap<K, V> = mutableMapOf(),
) : CacheStore<K, V> {
    private val loads = atomic(0)
    private val stores = atomic(0)
    private val removes = atomic(0)

    val loadCount: Int get() = loads.value
    val storeCount: Int get() = stores.value
    val removeCount: Int get() = removes.value

    override suspend fun load(key: K): V? {
        loads.incrementAndGet()
        return backing[key]
    }

    override suspend fun store(key: K, value: V) {
        stores.incrementAndGet()
        backing[key] = value
    }

    override suspend fun remove(key: K) {
        removes.incrementAndGet()
        backing.remove(key)
    }
}

/**
 * A [CacheStore] whose operations always fail, used to verify error handling and failure events.
 */
internal class FailingCacheStore<K, V> : CacheStore<K, V> {
    override suspend fun load(key: K): V = throw IllegalStateException("load failed")

    override suspend fun store(key: K, value: V) {
        throw IllegalStateException("store failed")
    }

    override suspend fun remove(key: K) {
        throw IllegalStateException("remove failed")
    }
}
