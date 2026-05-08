package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.constant.constantBackoff
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JitterBackoffStrategyBuilderTest : FunSpec({
    context("Variables") {
        test("default factor is 0.5") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            builder.factor shouldBe 0.5
        }

        test("setting factor via property updates correctly") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            builder.factor = 0.3
            builder.factor shouldBe 0.3
        }

        test("setting factor via function updates correctly") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            builder.factor(0.1)
            builder.factor shouldBe 0.1
        }

        test("factor of 1.0 is allowed") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            builder.factor = 1.0
            builder.factor shouldBe 1.0
        }

        test("factor of 0.0 throws IllegalArgumentException") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            shouldThrow<IllegalArgumentException> {
                builder.factor = 0.0
            }
        }

        test("negative factor throws IllegalArgumentException") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            shouldThrow<IllegalArgumentException> {
                builder.factor = -0.1
            }
        }

        test("factor above 1.0 throws IllegalArgumentException") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            shouldThrow<IllegalArgumentException> {
                builder.factor = 1.1
            }
        }

        test("factor out of range error message is correct") {
            val builder = JitterBackoffStrategyBuilder(constantBackoff())
            val ex = shouldThrow<IllegalArgumentException> {
                builder.factor(0.0)
            }
            ex.message shouldBe "factor must be between 0.0 (exclusive) and 1.0"
        }
    }
})