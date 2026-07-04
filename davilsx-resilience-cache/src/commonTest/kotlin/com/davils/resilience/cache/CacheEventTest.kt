package com.davils.resilience.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CacheEventTest : FunSpec({
    context("event payloads") {
        test("key-carrying events expose their key") {
            CacheEvent.CacheHit("a").key shouldBe "a"
            CacheEvent.CacheMiss("b").key shouldBe "b"
            CacheEvent.CachePut("c").key shouldBe "c"
            CacheEvent.CacheRemove("d").key shouldBe "d"
            CacheEvent.CacheEviction("e").key shouldBe "e"
            CacheEvent.CacheExpiration("f").key shouldBe "f"
            CacheEvent.CacheLoadSuccess("g").key shouldBe "g"
            CacheEvent.CacheWriteSuccess("h").key shouldBe "h"
        }

        test("failure events carry key and throwable") {
            val cause = IllegalStateException("boom")
            val loadFailure = CacheEvent.CacheLoadFailure("k", cause)
            loadFailure.key shouldBe "k"
            loadFailure.throwable shouldBe cause

            val writeFailure = CacheEvent.CacheWriteFailure("k", cause)
            writeFailure.throwable shouldBe cause
        }

        test("flush event carries the flushed count") {
            CacheEvent.CacheWriteBackFlushed(3).count shouldBe 3
        }

        test("singleton events are CacheEvent instances") {
            CacheEvent.CacheDispose.shouldBeInstanceOf<CacheEvent>()
            CacheEvent.CacheCleared.shouldBeInstanceOf<CacheEvent>()
        }

        test("value-equal key events are equal") {
            CacheEvent.CacheHit("same") shouldBe CacheEvent.CacheHit("same")
        }
    }
})
