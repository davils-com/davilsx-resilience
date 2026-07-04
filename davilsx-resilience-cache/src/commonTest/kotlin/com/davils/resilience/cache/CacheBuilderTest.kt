package com.davils.resilience.cache

import com.davils.kore.pattern.creational.dsl.verification.DslVerificationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheBuilderTest : FunSpec({
    context("defaults") {
        test("maxSize defaults to 1000") {
            CacheBuilder<String, String>().maxSize shouldBe 1000
        }

        test("evictionStrategy defaults to Lru") {
            CacheBuilder<String, String>().evictionStrategy shouldBe EvictionStrategy.Lru
        }

        test("expiration and cleanup default to zero") {
            val builder = CacheBuilder<String, String>()
            builder.expireAfterWrite shouldBe Duration.ZERO
            builder.expireAfterAccess shouldBe Duration.ZERO
            builder.cleanupInterval shouldBe Duration.ZERO
        }

        test("writeStrategy defaults to WRITE_THROUGH and store is null") {
            val builder = CacheBuilder<String, String>()
            builder.writeStrategy shouldBe WriteStrategy.WRITE_THROUGH
            builder.store shouldBe null
        }
    }

    context("setters") {
        test("maxSize setter") {
            val data = build { maxSize(50) }
            data.maxSize shouldBe 50
        }

        test("evictionStrategy enum setter maps to strategy") {
            build { evictionStrategy(EvictionStrategyType.LFU) }.evictionStrategy shouldBe EvictionStrategy.Lfu
            build { evictionStrategy(EvictionStrategyType.FIFO) }.evictionStrategy shouldBe EvictionStrategy.Fifo
        }

        test("custom eviction strategy setter") {
            val custom = EvictionStrategy.Fifo
            build { evictionStrategy(custom) }.evictionStrategy shouldBe custom
        }

        test("expireAfterWrite Duration and Long setters") {
            build { expireAfterWrite(2.seconds) }.expireAfterWrite shouldBe 2.seconds
            build { expireAfterWrite(500L) }.expireAfterWrite shouldBe 500.milliseconds
        }

        test("expireAfterAccess Duration and Long setters") {
            build { expireAfterAccess(3.seconds) }.expireAfterAccess shouldBe 3.seconds
            build { expireAfterAccess(750L) }.expireAfterAccess shouldBe 750.milliseconds
        }

        test("cleanupInterval Duration and Long setters") {
            build { cleanupInterval(4.seconds) }.cleanupInterval shouldBe 4.seconds
            build { cleanupInterval(1000L) }.cleanupInterval shouldBe 1000.milliseconds
        }

        test("store setter") {
            val store = FakeCacheStore<String, String>()
            build { store(store) }.store shouldBe store
        }

        test("writeStrategy setter") {
            build {
                store(FakeCacheStore())
                writeStrategy(WriteStrategy.WRITE_BACK)
            }.writeStrategy shouldBe WriteStrategy.WRITE_BACK
        }

        test("writeBack block configures the write-back config") {
            val data = build {
                store(FakeCacheStore())
                writeStrategy(WriteStrategy.WRITE_BACK)
                writeBack {
                    flushInterval(1.seconds)
                    batchSize(7)
                    flushOnDispose(false)
                }
            }
            data.writeBackConfig.batchSize shouldBe 7
            data.writeBackConfig.flushInterval shouldBe 1.seconds
            data.writeBackConfig.flushOnDispose shouldBe false
        }
    }

    context("produce validation") {
        test("maxSize below 1 throws") {
            shouldThrow<DslVerificationException> {
                CacheBuilder<String, String>().apply { maxSize(0) }.produce()
            }
        }

        test("write-back without a store throws") {
            shouldThrow<DslVerificationException> {
                CacheBuilder<String, String>().apply { writeStrategy(WriteStrategy.WRITE_BACK) }.produce()
            }
        }
    }
})

private fun build(block: CacheBuilder<String, String>.() -> Unit): CacheData<String, String> =
    CacheBuilder<String, String>().apply(block).produce()
