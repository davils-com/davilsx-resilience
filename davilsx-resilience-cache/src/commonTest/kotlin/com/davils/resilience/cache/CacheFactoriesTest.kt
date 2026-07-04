package com.davils.resilience.cache

import com.davils.resilience.cache.store.inMemoryCacheStore
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheFactoriesTest : FunSpec({
    context("eviction presets") {
        test("lruCache evicts the least recently used entry") {
            val cache = lruCache<String, String>(maxSize = 2)

            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe true
            cache.contains("b") shouldBe false
            cache.contains("c") shouldBe true

            cache.dispose()
        }

        test("lfuCache evicts the least frequently used entry") {
            val cache = lfuCache<String, String>(maxSize = 2)

            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe true
            cache.contains("b") shouldBe false
            cache.contains("c") shouldBe true

            cache.dispose()
        }

        test("fifoCache evicts the earliest inserted entry") {
            val cache = fifoCache<String, String>(maxSize = 2)

            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe false
            cache.contains("b") shouldBe true
            cache.contains("c") shouldBe true

            cache.dispose()
        }

        test("builder can override eviction strategy preset") {
            val cache = lfuCache<String, String>(maxSize = 2) {
                evictionStrategy(EvictionStrategyType.LRU)
            }

            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe true
            cache.contains("b") shouldBe false

            cache.dispose()
        }
    }

    context("write presets") {
        test("writeThroughCache persists puts synchronously") {
            val store = inMemoryCacheStore<String, String>()
            val cache = writeThroughCache(store)

            cache.put("k", "v")

            store.snapshot() shouldBe mapOf("k" to "v")
            cache.dispose()
        }

        test("writeBackCache buffers puts until flush") {
            val store = inMemoryCacheStore<String, String>()
            val cache = writeBackCache(store) {
                writeBack {
                    flushInterval(100.seconds)
                    batchSize(100)
                }
            }

            cache.put("k", "v")
            store.snapshot().isEmpty() shouldBe true

            cache.flush()
            store.snapshot() shouldBe mapOf("k" to "v")
            cache.dispose()
        }
    }

    context("expiringCache") {
        test("expires entries after write TTL") {
            val cache = expiringCache<String, String>(expireAfterWrite = 100.milliseconds)

            cache.put("token", "abc")
            cache.get("token") shouldBe "abc"

            delay(150)
            cache.get("token").shouldBeNull()

            cache.dispose()
        }
    }

    context("inMemoryCache") {
        test("creates a working cache without a store") {
            val cache = inMemoryCache<String, String> { maxSize(10) }

            cache.put("k", "v")
            cache.get("k") shouldBe "v"

            cache.dispose()
        }
    }
})
