package com.davils.resilience.retry.strategy.constant

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConstantBackoffStrategyBuilderTest : FunSpec({
    context("Variables") {
        test("default delay is 1000 milliseconds") {
            val builder = ConstantBackoffStrategyBuilder()
            builder.delay shouldBe 1000.milliseconds
        }

        test("setting delay via Duration updates the value") {
            val builder = ConstantBackoffStrategyBuilder()
            builder.delay = 2.seconds
            builder.delay shouldBe 2.seconds
        }

        test("setting delay via Long milliseconds updates the value") {
            val builder = ConstantBackoffStrategyBuilder()
            builder.delay(750L)
            builder.delay shouldBe 750.milliseconds
        }

        test("setting delay via Duration function updates the value") {
            val builder = ConstantBackoffStrategyBuilder()
            builder.delay(3.seconds)
            builder.delay shouldBe 3.seconds
        }

        test("setting delay to zero is allowed") {
            val builder = ConstantBackoffStrategyBuilder()
            builder.delay = Duration.ZERO
            builder.delay shouldBe Duration.ZERO
        }

        test("setting a negative delay throws IllegalArgumentException") {
            val builder = ConstantBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.delay = (-1).milliseconds
            }
        }

        test("negative delay via Long throws IllegalArgumentException") {
            val builder = ConstantBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.delay(-100L)
            }
        }

        test("negative delay error message is correct") {
            val builder = ConstantBackoffStrategyBuilder()
            val ex = shouldThrow<IllegalArgumentException> {
                builder.delay = (-500).milliseconds
            }
            ex.message shouldBe "delay must be non-negative"
        }
    }

    context("Functions") {
        test("build produces ConstantBackoffStrategyData with configured delay") {
            val strategy = constantBackoff { delay(400L) }
            strategy.calculateDelay(1) shouldBe 400.milliseconds
        }

        test("build uses default delay when nothing is configured") {
            val strategy = constantBackoff {}
            strategy.calculateDelay(1) shouldBe 1000.milliseconds
        }
    }
})
