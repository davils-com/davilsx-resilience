package com.davils.resilience.cache.store

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.atomicfu.atomic

class CacheStoreTest : FunSpec({
    context("InMemoryCacheStore") {
        test("load returns null for an empty store") {
            val store = inMemoryCacheStore<String, String>()
            store.load("missing").shouldBeNull()
        }

        test("store load and remove roundtrip") {
            val store = inMemoryCacheStore<String, String>()

            store.store("k", "v")
            store.load("k") shouldBe "v"

            store.remove("k")
            store.load("k").shouldBeNull()
        }

        test("initial entries are available after first access") {
            val store = inMemoryCacheStore(initial = mapOf("a" to "1", "b" to "2"))

            store.load("a") shouldBe "1"
            store.load("b") shouldBe "2"
        }

        test("snapshot reflects the current store contents") {
            val store = inMemoryCacheStore(initial = mapOf("a" to "1"))

            store.store("b", "2")

            store.snapshot() shouldBe mapOf("a" to "1", "b" to "2")
        }
    }

    context("DelegatingCacheStore") {
        test("delegates load store and remove to the provided functions") {
            val backing = mutableMapOf<String, String>()
            val loadCalls = atomic(0)
            val storeCalls = atomic(0)
            val removeCalls = atomic(0)

            val store = delegatingCacheStore<String, String>(
                load = { key ->
                    loadCalls.incrementAndGet()
                    backing[key]
                },
                store = { key, value ->
                    storeCalls.incrementAndGet()
                    backing[key] = value
                },
                remove = { key ->
                    removeCalls.incrementAndGet()
                    backing.remove(key)
                },
            )

            store.load("missing").shouldBeNull()
            loadCalls.value shouldBe 1

            store.store("k", "v")
            storeCalls.value shouldBe 1

            store.load("k") shouldBe "v"
            loadCalls.value shouldBe 2

            store.remove("k")
            removeCalls.value shouldBe 1
            store.load("k").shouldBeNull()
        }
    }
})
