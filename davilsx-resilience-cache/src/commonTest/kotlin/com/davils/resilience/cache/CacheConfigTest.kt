package com.davils.resilience.cache

import com.davils.kore.pattern.creational.dsl.verification.DslVerificationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheConfigTest : FunSpec({
    context("WriteBackConfigBuilder defaults") {
        test("default flushInterval is 5 seconds") {
            WriteBackConfigBuilder().flushInterval shouldBe 5.seconds
        }

        test("default batchSize is 100") {
            WriteBackConfigBuilder().batchSize shouldBe 100
        }

        test("default flushOnDispose is true") {
            WriteBackConfigBuilder().flushOnDispose shouldBe true
        }
    }

    context("WriteBackConfigBuilder setters") {
        test("flushInterval Duration setter") {
            val config = writeBackConfig { flushInterval(2.seconds) }
            config.flushInterval shouldBe 2.seconds
        }

        test("flushInterval Long millis setter") {
            val config = writeBackConfig { flushInterval(250L) }
            config.flushInterval shouldBe 250.milliseconds
        }

        test("batchSize setter") {
            val config = writeBackConfig { batchSize(10) }
            config.batchSize shouldBe 10
        }

        test("flushOnDispose setter") {
            val config = writeBackConfig { flushOnDispose(false) }
            config.flushOnDispose shouldBe false
        }
    }

    context("validation") {
        test("zero flushInterval fails validation") {
            shouldThrow<DslVerificationException> {
                writeBackConfig { flushInterval(Duration.ZERO) }
            }
        }

        test("negative flushInterval fails validation") {
            shouldThrow<DslVerificationException> {
                writeBackConfig { flushInterval((-1).seconds) }
            }
        }

        test("zero batchSize fails validation") {
            shouldThrow<DslVerificationException> {
                writeBackConfig { batchSize(0) }
            }
        }

        test("valid config passes validation") {
            val config = WriteBackConfig(flushInterval = 1.seconds, batchSize = 1, flushOnDispose = true)
            config.validate().isValid shouldBe true
        }

        test("invalid config surfaces the failing fields") {
            val config = WriteBackConfig(flushInterval = Duration.ZERO, batchSize = 0, flushOnDispose = true)
            val failures = config.validate().failures
            failures.map { it.field } shouldBe listOf("flushInterval", "batchSize")
        }
    }
})
