package com.davils.resilience.cache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheTest : FunSpec({
    context("basic operations") {
        test("put then get returns the stored value") {
            val cache = cache<String, String> { }
            cache.put("k", "v")
            cache.get("k") shouldBe "v"
        }

        test("get returns null for an unknown key") {
            val cache = cache<String, String> { }
            cache.get("missing").shouldBeNull()
        }

        test("getOrNull returns the value without loading from a store") {
            val store = FakeCacheStore(mutableMapOf("k" to "fromStore"))
            val cache = cache<String, String> { store(store) }
            cache.getOrNull("k").shouldBeNull()
            store.loadCount shouldBe 0
        }

        test("put overwrites an existing value") {
            val cache = cache<String, String> { }
            cache.put("k", "v1")
            cache.put("k", "v2")
            cache.get("k") shouldBe "v2"
        }

        test("contains reflects presence") {
            val cache = cache<String, String> { }
            cache.contains("k") shouldBe false
            cache.put("k", "v")
            cache.contains("k") shouldBe true
        }

        test("remove deletes an entry") {
            val cache = cache<String, String> { }
            cache.put("k", "v")
            cache.remove("k")
            cache.contains("k") shouldBe false
        }

        test("size and keys reflect contents") {
            val cache = cache<String, String> { }
            cache.put("a", "1")
            cache.put("b", "2")
            cache.size() shouldBe 2L
            cache.keys() shouldContainExactlyInAnyOrder listOf("a", "b")
        }

        test("clear removes all entries") {
            val cache = cache<String, String> { }
            cache.put("a", "1")
            cache.put("b", "2")
            cache.clear()
            cache.size() shouldBe 0L
        }
    }

    context("loader-based get") {
        test("loads and caches on miss") {
            val cache = cache<String, String> { }
            var calls = 0
            val loaded = cache.get("k") { calls++; "loaded" }
            loaded shouldBe "loaded"
            calls shouldBe 1

            val second = cache.get("k") { calls++; "again" }
            second shouldBe "loaded"
            calls shouldBe 1
        }
    }

    context("eviction") {
        test("FIFO evicts the earliest inserted entry") {
            val cache = cache<String, String> {
                maxSize(2)
                evictionStrategy(EvictionStrategyType.FIFO)
            }
            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("c", "3")

            cache.size() shouldBe 2L
            cache.contains("a") shouldBe false
            cache.contains("b") shouldBe true
            cache.contains("c") shouldBe true
        }

        test("LRU evicts the least recently used entry") {
            val cache = cache<String, String> {
                maxSize(2)
                evictionStrategy(EvictionStrategyType.LRU)
            }
            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe true
            cache.contains("b") shouldBe false
            cache.contains("c") shouldBe true
        }

        test("LFU evicts the least frequently used entry") {
            val cache = cache<String, String> {
                maxSize(2)
                evictionStrategy(EvictionStrategyType.LFU)
            }
            cache.put("a", "1")
            cache.put("b", "2")
            cache.get("a")
            cache.get("a")
            cache.put("c", "3")

            cache.contains("a") shouldBe true
            cache.contains("b") shouldBe false
            cache.contains("c") shouldBe true
        }
    }

    context("TTL expiration") {
        test("expireAfterWrite expires entries lazily on access") {
            val cache = cache<String, String> { expireAfterWrite(100.milliseconds) }
            cache.put("k", "v")
            cache.get("k") shouldBe "v"
            delay(200)
            cache.get("k").shouldBeNull()
        }

        test("expireAfterAccess is refreshed by access") {
            val cache = cache<String, String> { expireAfterAccess(150.milliseconds) }
            cache.put("k", "v")
            delay(80)
            cache.get("k") shouldBe "v"
            delay(80)
            cache.get("k") shouldBe "v"
            delay(250)
            cache.get("k").shouldBeNull()
        }

        test("active cleanup removes expired entries without access") {
            val cache = cache<String, String> {
                expireAfterWrite(50.milliseconds)
                cleanupInterval(50.milliseconds)
            }
            cache.put("k", "v")
            withTimeout(2.seconds) {
                while (cache.size() > 0L) delay(20)
            }
            cache.size() shouldBe 0L
        }
    }

    context("read-through store") {
        test("loads from the store on a miss and caches the result") {
            val store = FakeCacheStore(mutableMapOf("k" to "fromStore"))
            val cache = cache<String, String> { store(store) }

            cache.get("k") shouldBe "fromStore"
            store.loadCount shouldBe 1

            cache.get("k") shouldBe "fromStore"
            store.loadCount shouldBe 1
        }

        test("load failure returns null and does not throw") {
            val cache = cache<String, String> { store(FailingCacheStore()) }
            cache.get("k").shouldBeNull()
        }
    }

    context("write-through store") {
        test("put writes through to the store synchronously") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> { store(store) }

            cache.put("k", "v")
            store.backing["k"] shouldBe "v"
            store.storeCount shouldBe 1
        }

        test("remove mirrors to the store") {
            val store = FakeCacheStore(mutableMapOf("k" to "v"))
            val cache = cache<String, String> { store(store) }
            cache.put("k", "v")

            cache.remove("k")
            store.backing.containsKey("k") shouldBe false
        }

        test("write failure propagates from put") {
            val cache = cache<String, String> { store(FailingCacheStore()) }
            shouldThrow<IllegalStateException> { cache.put("k", "v") }
        }
    }

    context("write-back store") {
        test("buffers writes until flushed manually") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> {
                store(store)
                writeStrategy(WriteStrategy.WRITE_BACK)
                writeBack {
                    flushInterval(100.seconds)
                    batchSize(100)
                }
            }

            cache.put("a", "1")
            cache.put("b", "2")
            store.backing.isEmpty() shouldBe true

            cache.flush()
            store.backing["a"] shouldBe "1"
            store.backing["b"] shouldBe "2"
        }

        test("auto-flushes when the batch size is reached") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> {
                store(store)
                writeStrategy(WriteStrategy.WRITE_BACK)
                writeBack {
                    flushInterval(100.seconds)
                    batchSize(2)
                }
            }

            cache.put("a", "1")
            cache.put("b", "2")

            store.backing["a"] shouldBe "1"
            store.backing["b"] shouldBe "2"
        }

        test("flushes on dispose when configured") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> {
                store(store)
                writeStrategy(WriteStrategy.WRITE_BACK)
                writeBack {
                    flushInterval(100.seconds)
                    batchSize(100)
                    flushOnDispose(true)
                }
            }

            cache.put("a", "1")
            cache.dispose()

            store.backing["a"] shouldBe "1"
        }

        test("does not flush on dispose when disabled") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> {
                store(store)
                writeStrategy(WriteStrategy.WRITE_BACK)
                writeBack {
                    flushInterval(100.seconds)
                    batchSize(100)
                    flushOnDispose(false)
                }
            }

            cache.put("a", "1")
            cache.dispose()

            store.backing.isEmpty() shouldBe true
        }
    }

    context("disposal") {
        test("operations fail after disposal") {
            val cache = cache<String, String> { }
            cache.dispose()
            shouldThrow<CancellationException> { cache.get("k") }
            shouldThrow<CancellationException> { cache.put("k", "v") }
        }

        test("isDisposed reports state") {
            val cache = cache<String, String> { }
            cache.isDisposed() shouldBe false
            cache.dispose()
            cache.isDisposed() shouldBe true
        }
    }

    context("concurrency") {
        test("concurrent puts are all stored") {
            val cache = cache<Int, Int> { maxSize(1000) }
            coroutineScope {
                repeat(100) { i ->
                    launch { cache.put(i, i) }
                }
            }
            cache.size() shouldBe 100L
        }
    }

    context("events") {
        test("emits hit, miss and put events") {
            val cache = cache<String, String> { event { replay = 100 } }
            val received = Channel<CacheEvent>(Channel.UNLIMITED)
            val job = cache.subscribe<CacheEvent> { received.trySend(it) }

            cache.put("k", "v")
            cache.get("k")
            cache.get("missing")

            val events = withTimeout(2.seconds) {
                buildList {
                    while (true) {
                        add(received.receive())
                        if (any { it is CacheEvent.CachePut<*> } &&
                            any { it is CacheEvent.CacheHit<*> } &&
                            any { it is CacheEvent.CacheMiss<*> }
                        ) {
                            break
                        }
                    }
                }
            }
            job.cancel()

            events.filterIsInstance<CacheEvent.CachePut<*>>().map { it.key } shouldContainExactlyInAnyOrder listOf("k")
            events.filterIsInstance<CacheEvent.CacheHit<*>>().map { it.key } shouldContainExactlyInAnyOrder listOf("k")
            events.filterIsInstance<CacheEvent.CacheMiss<*>>().map { it.key } shouldContainExactlyInAnyOrder listOf("missing")
        }

        test("emits an eviction event") {
            val cache = cache<String, String> {
                maxSize(1)
                evictionStrategy(EvictionStrategyType.FIFO)
                event { replay = 100 }
            }
            val received = Channel<CacheEvent>(Channel.UNLIMITED)
            val job = cache.subscribe<CacheEvent.CacheEviction<*>> { received.trySend(it) }

            cache.put("a", "1")
            cache.put("b", "2")

            val eviction = withTimeout(2.seconds) { received.receive() }
            job.cancel()

            (eviction as CacheEvent.CacheEviction<*>).key shouldBe "a"
        }
    }

    context("get with loader and store") {
        test("prefers the store over the loader") {
            val store = FakeCacheStore(mutableMapOf("k" to "fromStore"))
            val cache = cache<String, String> { store(store) }
            var loaderCalled = false

            val value = cache.get("k") { loaderCalled = true; "fromLoader" }

            value shouldBe "fromStore"
            loaderCalled shouldBe false
        }

        test("falls back to the loader when the store has no value") {
            val store = FakeCacheStore<String, String>()
            val cache = cache<String, String> { store(store) }

            val value = cache.get("k") { "fromLoader" }

            value shouldBe "fromLoader"
            cache.getOrNull("k").shouldNotBeNull()
        }
    }
})
