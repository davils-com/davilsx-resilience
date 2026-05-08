package com.davils.resilience.retry.strategy.exponential

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffStrategyBuilderTest : FunSpec({
    context("Variables") {
        test("default initialDelay is 1000 milliseconds") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.initialDelay shouldBe 1000.milliseconds
        }

        test("default maxDelay is 60000 milliseconds") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.maxDelay shouldBe 60000.milliseconds
        }

        test("default multiplier is 2.0") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.multiplier shouldBe 2.0
        }

        test("setting initialDelay via Duration updates correctly") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.initialDelay = 2.seconds
            builder.initialDelay shouldBe 2.seconds
        }

        test("setting initialDelay via Long updates correctly") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.initialDelay(750L)
            builder.initialDelay shouldBe 750.milliseconds
        }

        test("setting maxDelay via Duration updates correctly") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.maxDelay = 30.seconds
            builder.maxDelay shouldBe 30.seconds
        }

        test("setting maxDelay via Long updates correctly") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.maxDelay(30000L)
            builder.maxDelay shouldBe 30000.milliseconds
        }

        test("setting multiplier updates correctly") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.multiplier(3.0)
            builder.multiplier shouldBe 3.0
        }

        test("setting zero initialDelay is allowed") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.initialDelay = Duration.ZERO
            builder.initialDelay shouldBe Duration.ZERO
        }

        test("setting zero maxDelay is allowed") {
            val builder = ExponentialBackoffStrategyBuilder()
            builder.maxDelay = Duration.ZERO
            builder.maxDelay shouldBe Duration.ZERO
        }

        test("negative initialDelay throws IllegalArgumentException") {
            val builder = ExponentialBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.initialDelay = (-1).milliseconds
            }
        }

        test("negative initialDelay error message is correct") {
            val builder = ExponentialBackoffStrategyBuilder()
            val ex = shouldThrow<IllegalArgumentException> {
                builder.initialDelay = (-100).milliseconds
            }
            ex.message shouldBe "initialDelay must be non-negative"
        }

        test("negative maxDelay throws IllegalArgumentException") {
            val builder = ExponentialBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.maxDelay = (-1).milliseconds
            }
        }

        test("negative maxDelay error message is correct") {
            val builder = ExponentialBackoffStrategyBuilder()
            val ex = shouldThrow<IllegalArgumentException> {
                builder.maxDelay = (-500).milliseconds
            }
            ex.message shouldBe "maxDelay must be non-negative"
        }

        test("zero multiplier throws IllegalArgumentException") {
            val builder = ExponentialBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.multiplier = 0.0
            }
        }

        test("negative multiplier throws IllegalArgumentException") {
            val builder = ExponentialBackoffStrategyBuilder()
            shouldThrow<IllegalArgumentException> {
                builder.multiplier = -1.0
            }
        }

        test("negative multiplier error message is correct") {
            val builder = ExponentialBackoffStrategyBuilder()
            val ex = shouldThrow<IllegalArgumentException> {
                builder.multiplier(-0.5)
            }
            ex.message shouldBe "multiplier must be greater than 0"
        }
    }
})
