package com.davils.resilience.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheEntryTest : FunSpec({
    context("create") {
        test("initializes metadata with a single access and equal timestamps") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 7L)

            entry.value shouldBe "v"
            entry.accessCount shouldBe 1L
            entry.insertionSeq shouldBe 7L
            entry.createdAt shouldBe entry.lastAccessedAt
        }
    }

    context("accessed") {
        test("increments access count and preserves value, creation time and sequence") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 3L)

            val accessed = entry.accessed()

            accessed.value shouldBe "v"
            accessed.accessCount shouldBe 2L
            accessed.insertionSeq shouldBe 3L
            accessed.createdAt shouldBe entry.createdAt
        }

        test("accumulates access count across multiple accesses") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)

            val result = entry.accessed().accessed().accessed()

            result.accessCount shouldBe 4L
        }

        test("refreshes access-based expiration") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)
            delay(120)

            val refreshed = entry.accessed()

            refreshed.isExpired(Duration.ZERO, 100.milliseconds) shouldBe false
        }
    }

    context("isExpired") {
        test("is not expired when both TTLs are disabled") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)
            delay(50)

            entry.isExpired(Duration.ZERO, Duration.ZERO) shouldBe false
        }

        test("expires after write TTL elapses") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)
            delay(120)

            entry.isExpired(expireAfterWrite = 100.milliseconds, expireAfterAccess = Duration.ZERO) shouldBe true
        }

        test("is not expired before write TTL elapses") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)

            entry.isExpired(expireAfterWrite = 10.seconds, expireAfterAccess = Duration.ZERO) shouldBe false
        }

        test("expires after access TTL elapses") {
            val entry = CacheEntry.create(value = "v", insertionSeq = 0L)
            delay(120)

            entry.isExpired(expireAfterWrite = Duration.ZERO, expireAfterAccess = 100.milliseconds) shouldBe true
        }
    }
})
