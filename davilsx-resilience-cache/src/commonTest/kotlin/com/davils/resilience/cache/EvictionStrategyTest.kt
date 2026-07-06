package com.davils.resilience.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

class EvictionStrategyTest : FunSpec({
    context("Lru") {
        test("selects the entry that was accessed least recently") {
            val old = CacheEntry.create(value = "a", insertionSeq = 0L)
            delay(20.milliseconds)
            val recent = CacheEntry.create(value = "b", insertionSeq = 1L)

            val victim = EvictionStrategy.Lru.selectVictim(mapOf("a" to old, "b" to recent))

            victim shouldBe "a"
        }

        test("respects a refreshed access time") {
            val first = CacheEntry.create(value = "a", insertionSeq = 0L)
            delay(20.milliseconds)
            val second = CacheEntry.create(value = "b", insertionSeq = 1L)
            delay(20.milliseconds)
            val refreshedFirst = first.accessed()

            val victim = EvictionStrategy.Lru.selectVictim(mapOf("a" to refreshedFirst, "b" to second))

            victim shouldBe "b"
        }

        test("returns null for an empty map") {
            EvictionStrategy.Lru.selectVictim(emptyMap<String, CacheEntry<*>>()).shouldBeNull()
        }

        test("breaks elapsed-time ties by access count when the clock does not advance") {
            val accessedAt = TimeSource.Monotonic.markNow()
            val entries = mapOf(
                "a" to entry(accessedAt = accessedAt, insertionSeq = 0L, accessCount = 2L),
                "b" to entry(accessedAt = accessedAt, insertionSeq = 1L, accessCount = 1L),
            )

            EvictionStrategy.Lru.selectVictim(entries) shouldBe "b"
        }
    }

    context("Lfu") {
        test("selects the entry with the fewest accesses") {
            val entries = mapOf(
                "a" to entry(insertionSeq = 0L, accessCount = 5L),
                "b" to entry(insertionSeq = 1L, accessCount = 1L),
                "c" to entry(insertionSeq = 2L, accessCount = 3L),
            )

            EvictionStrategy.Lfu.selectVictim(entries) shouldBe "b"
        }

        test("breaks frequency ties by insertion order") {
            val entries = mapOf(
                "a" to entry(insertionSeq = 2L, accessCount = 1L),
                "b" to entry(insertionSeq = 0L, accessCount = 1L),
                "c" to entry(insertionSeq = 1L, accessCount = 1L),
            )

            EvictionStrategy.Lfu.selectVictim(entries) shouldBe "b"
        }

        test("returns null for an empty map") {
            EvictionStrategy.Lfu.selectVictim(emptyMap<String, CacheEntry<*>>()).shouldBeNull()
        }
    }

    context("Fifo") {
        test("selects the earliest inserted entry regardless of access") {
            val entries = mapOf(
                "a" to entry(insertionSeq = 2L, accessCount = 1L),
                "b" to entry(insertionSeq = 0L, accessCount = 99L),
                "c" to entry(insertionSeq = 1L, accessCount = 50L),
            )

            EvictionStrategy.Fifo.selectVictim(entries) shouldBe "b"
        }

        test("returns null for an empty map") {
            EvictionStrategy.Fifo.selectVictim(emptyMap<String, CacheEntry<*>>()).shouldBeNull()
        }
    }

    context("toStrategy") {
        test("maps LRU to Lru") {
            EvictionStrategyType.LRU.toStrategy() shouldBe EvictionStrategy.Lru
        }

        test("maps LFU to Lfu") {
            EvictionStrategyType.LFU.toStrategy() shouldBe EvictionStrategy.Lfu
        }

        test("maps FIFO to Fifo") {
            EvictionStrategyType.FIFO.toStrategy() shouldBe EvictionStrategy.Fifo
        }
    }
})

private fun entry(
    insertionSeq: Long,
    accessCount: Long,
    accessedAt: kotlin.time.TimeMark = TimeSource.Monotonic.markNow(),
): CacheEntry<String> {
    return CacheEntry(
        value = "value-$insertionSeq",
        createdAt = accessedAt,
        lastAccessedAt = accessedAt,
        accessCount = accessCount,
        insertionSeq = insertionSeq,
    )
}
