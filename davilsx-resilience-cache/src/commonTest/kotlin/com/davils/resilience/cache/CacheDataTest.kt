package com.davils.resilience.cache

import com.davils.resilience.common.event.ResilienceEventBuilder
import com.davils.resilience.common.event.ResilienceEventData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheDataTest : FunSpec({
    fun eventData(): ResilienceEventData = ResilienceEventBuilder().produce()

    fun data(
        maxSize: Int = 100,
        expireAfterWrite: Duration = Duration.ZERO,
        expireAfterAccess: Duration = Duration.ZERO,
        cleanupInterval: Duration = Duration.ZERO,
        store: CacheStore<String, String>? = null,
        writeStrategy: WriteStrategy = WriteStrategy.WRITE_THROUGH,
        writeBackConfig: WriteBackConfig = WriteBackConfig(1.seconds, 1, true),
    ): CacheData<String, String> = CacheData(
        eventData = eventData(),
        maxSize = maxSize,
        evictionStrategy = EvictionStrategy.Lru,
        expireAfterWrite = expireAfterWrite,
        expireAfterAccess = expireAfterAccess,
        store = store,
        writeStrategy = writeStrategy,
        writeBackConfig = writeBackConfig,
        cleanupInterval = cleanupInterval,
    )

    context("validate") {
        test("a default in-memory configuration is valid") {
            data().validate().isValid shouldBe true
        }

        test("maxSize below 1 fails") {
            val failures = data(maxSize = 0).validate().failures
            failures.map { it.field } shouldContain "maxSize"
        }

        test("negative expireAfterWrite fails") {
            val failures = data(expireAfterWrite = (-1).milliseconds).validate().failures
            failures.map { it.field } shouldContain "expireAfterWrite"
        }

        test("negative expireAfterAccess fails") {
            val failures = data(expireAfterAccess = (-1).milliseconds).validate().failures
            failures.map { it.field } shouldContain "expireAfterAccess"
        }

        test("negative cleanupInterval fails") {
            val failures = data(cleanupInterval = (-1).milliseconds).validate().failures
            failures.map { it.field } shouldContain "cleanupInterval"
        }

        test("write-back without a store fails") {
            val failures = data(writeStrategy = WriteStrategy.WRITE_BACK, store = null).validate().failures
            failures.map { it.field } shouldContain "store"
        }

        test("write-back with a store is valid") {
            val valid = data(
                writeStrategy = WriteStrategy.WRITE_BACK,
                store = FakeCacheStore(),
            ).validate().isValid
            valid shouldBe true
        }

        test("write-back propagates write-back config failures") {
            val failures = data(
                writeStrategy = WriteStrategy.WRITE_BACK,
                store = FakeCacheStore(),
                writeBackConfig = WriteBackConfig(Duration.ZERO, 0, true),
            ).validate().failures
            failures.map { it.field } shouldContain "flushInterval"
            failures.map { it.field } shouldContain "batchSize"
        }

        test("write-through does not require a store") {
            data(writeStrategy = WriteStrategy.WRITE_THROUGH, store = null).validate().isValid shouldBe true
        }
    }
})
