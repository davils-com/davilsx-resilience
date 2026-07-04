package com.davils.resilience.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CacheRegistryTest : FunSpec({
    context("factory and creation") {
        test("creates a standalone cache from defaults") {
            val registry = cacheRegistry<String, String> { }
            val cache = registry.create { maxSize(10) }
            cache.put("k", "v")
            cache.get("k") shouldBe "v"
        }

        test("getOrCreate registers and reuses the same instance") {
            val registry = cacheRegistry<String, String> { }

            val first = registry.getOrCreate("user-cache") { maxSize(5) }
            val second = registry.getOrCreate("user-cache") { maxSize(5) }

            first shouldBeSameInstanceAs second
            registry.size() shouldBe 1
        }

        test("lookup returns a registered cache") {
            val registry = cacheRegistry<String, String> { }
            val created = registry.create("named-cache")

            registry.lookup("named-cache") shouldBeSameInstanceAs created
        }

        test("lookupOrNull returns null for an unknown name") {
            val registry = cacheRegistry<String, String> { }
            registry.lookupOrNull("absent").shouldBeNull()
        }

        test("remove deletes a registered cache") {
            val registry = cacheRegistry<String, String> { }
            registry.create("temp")
            registry.exists("temp") shouldBe true

            registry.remove("temp")
            registry.exists("temp") shouldBe false
        }

        test("default configuration is applied to created caches") {
            val registry = cacheRegistry<String, String> { }
            registry.default { maxSize(3) }
            val cache = registry.create("with-default")

            cache.put("a", "1")
            cache.put("b", "2")
            cache.put("c", "3")
            cache.put("d", "4")

            cache.size() shouldBe 3L
        }
    }
})
