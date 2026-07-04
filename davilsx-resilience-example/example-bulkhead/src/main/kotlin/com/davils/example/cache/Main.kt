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

package com.davils.example.cache

import com.davils.resilience.cache.Cache
import com.davils.resilience.cache.CacheEvent
import com.davils.resilience.cache.CacheStore
import com.davils.resilience.cache.EvictionStrategyType
import com.davils.resilience.cache.WriteStrategy
import com.davils.resilience.cache.cache
import com.davils.resilience.cache.cacheRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A trivial in-memory [CacheStore] that simulates a slow backing database.
 */
private class InMemoryUserStore : CacheStore<String, String> {
    private val database = mutableMapOf("u1" to "Ada Lovelace")

    override suspend fun load(key: String): String? {
        delay(50)
        return database[key]
    }

    override suspend fun store(key: String, value: String) {
        delay(50)
        database[key] = value
    }

    override suspend fun remove(key: String) {
        database.remove(key)
    }

    fun snapshot(): Map<String, String> = database.toMap()
}

suspend fun main() {
    basicUsageExample()
    loaderExample()
    evictionExample()
    expirationExample()
    readThroughWriteThroughExample()
    writeBackExample()
    eventsExample()
    registryExample()
}

/**
 * Basic put/get, containment and removal against a simple in-memory cache.
 */
private suspend fun basicUsageExample() {
    println("--- basic usage ---")

    val cache = cache<String, String> {
        maxSize = 100
        maxSize(100)
    }

    cache.put("key", "value")
    println("get(key) = ${cache.get("key")}")
    println("contains(key) = ${cache.contains("key")}")
    println("size = ${cache.size()}")

    cache.remove("key")
    println("get(key) after remove = ${cache.get("key")}")

    cache.dispose()
}

/**
 * Uses the loader overload of [Cache.get] to compute and cache values on a miss.
 */
private suspend fun loaderExample() {
    println("\n--- loader ---")

    val cache = cache<Int, Int> { maxSize(64) }

    val squared = cache.get(9) { key ->
        println("computing square of $key")
        key * key
    }
    println("squared = $squared")

    val cached = cache.get(9) { key -> key * key }
    println("cached (loader not invoked) = $cached")

    cache.dispose()
}

/**
 * Demonstrates size-bounded eviction using the LRU strategy.
 */
private suspend fun evictionExample() {
    println("\n--- eviction (LRU) ---")

    val cache = cache<String, String> {
        maxSize(2)
        evictionStrategy(EvictionStrategyType.LRU)
    }

    cache.put("a", "1")
    cache.put("b", "2")
    cache.get("a")
    cache.put("c", "3")

    println("contains(a) = ${cache.contains("a")}")
    println("contains(b) = ${cache.contains("b")} (evicted as least recently used)")
    println("contains(c) = ${cache.contains("c")}")

    cache.dispose()
}

/**
 * Shows lazy write-based TTL expiration together with active background cleanup.
 */
private suspend fun expirationExample() {
    println("\n--- expiration (TTL) ---")

    val cache = cache<String, String> {
        expireAfterWrite(200.milliseconds)
        cleanupInterval(100.milliseconds)
    }

    cache.put("token", "abc123")
    println("immediately after put = ${cache.get("token")}")

    delay(300)
    println("after TTL elapsed = ${cache.get("token")}")

    cache.dispose()
}

/**
 * Combines read-through loading and synchronous write-through persistence.
 */
private suspend fun readThroughWriteThroughExample() {
    println("\n--- read-through / write-through ---")

    val store = InMemoryUserStore()
    val cache = cache<String, String> {
        store(store)
        writeStrategy(WriteStrategy.WRITE_THROUGH)
    }

    println("read-through miss loaded from store = ${cache.get("u1")}")

    cache.put("u2", "Grace Hopper")
    println("store after write-through = ${store.snapshot()}")

    cache.dispose()
}

/**
 * Buffers writes with the write-back strategy and flushes them in batches.
 */
private suspend fun writeBackExample() {
    println("\n--- write-back ---")

    val store = InMemoryUserStore()
    val cache = cache<String, String> {
        store(store)
        writeStrategy(WriteStrategy.WRITE_BACK)
        writeBack {
            flushInterval(2.seconds)
            batchSize(10)
            flushOnDispose(true)
        }
    }

    cache.put("u3", "Alan Turing")
    cache.put("u4", "Edsger Dijkstra")
    println("store before flush = ${store.snapshot()}")

    cache.flush()
    println("store after manual flush = ${store.snapshot()}")

    cache.dispose()
}

/**
 * Subscribes to the cache event stream for observability.
 */
private suspend fun eventsExample() {
    println("\n--- events ---")

    val cache = cache<String, String> {
        maxSize(1)
        evictionStrategy(EvictionStrategyType.FIFO)
        event {
            scope = CoroutineScope(Dispatchers.Default)
            replay = 0
            overflowStrategy = BufferOverflow.DROP_OLDEST
            extraBufferCapacity = 128
            onError = { throwable -> println("event error: $throwable") }
        }
    }

    cache.subscribe(CacheEvent::class) { event -> println("event: ${event::class.simpleName}") }

    cache.put("a", "1")
    cache.put("b", "2")
    cache.get("b")
    cache.get("missing")

    delay(100)
    cache.dispose()
}

/**
 * Manages named caches through a [cacheRegistry].
 */
private suspend fun registryExample() {
    println("\n--- registry ---")

    val registry = cacheRegistry<String, String> { }
    registry.default { maxSize(500) }

    val sessions = registry.getOrCreate("sessions") { expireAfterAccess(30.seconds) }
    sessions.put("session-1", "user-42")

    val sameInstance = registry.getOrCreate("sessions")
    println("registry reuses instance = ${sessions === sameInstance}")
    println("session-1 = ${sameInstance.get("session-1")}")
    println("registry size = ${registry.size()}")

    registry.dispose()
}
