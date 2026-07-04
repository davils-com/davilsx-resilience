package com.davils.resilience.metrics.cache

import com.davils.resilience.cache.cache
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CacheMetricsCollectorTest : FunSpec({
    test("allMetrics reflects hits and misses") {
        val cache = cache<String, String> { }
        cache.put("k", "v")
        cache.get("k")
        cache.get("missing")

        val snapshot = cache.metrics.allMetrics()
        snapshot.numberOfHits shouldBe 1
        snapshot.numberOfMisses shouldBe 1
        snapshot.numberOfPuts shouldBe 1
        snapshot.currentSize shouldBe 1
        snapshot.hitRate shouldBe 50f
    }

    test("metrics extension provides collector access") {
        val cache = cache<String, String> { }
        cache.put("a", "1")
        cache.get("a")

        cache.metrics().allMetrics().numberOfHits shouldBe 1

        var configured = false
        cache.metrics { configured = true }
        configured shouldBe true
    }
})
