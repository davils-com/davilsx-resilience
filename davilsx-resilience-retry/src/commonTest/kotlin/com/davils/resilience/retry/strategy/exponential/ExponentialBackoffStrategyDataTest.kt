package com.davils.resilience.retry.strategy.exponential

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffStrategyDataTest : FunSpec({
    context("Variables") {
        test("stores initialDelay correctly") {
            val data = ExponentialBackoffStrategyData(
                maxDelay = 60.seconds,
                multiplier = 2.0,
                initialDelay = 1.seconds
            )
            data.initialDelay shouldBe 1.seconds
        }

        test("stores maxDelay correctly") {
            val data = ExponentialBackoffStrategyData(
                maxDelay = 30.seconds,
                multiplier = 2.0,
                initialDelay = 1.seconds
            )
            data.maxDelay shouldBe 30.seconds
        }

        test("stores multiplier correctly") {
            val data = ExponentialBackoffStrategyData(
                maxDelay = 60.seconds,
                multiplier = 3.5,
                initialDelay = 1.seconds
            )
            data.multiplier shouldBe 3.5
        }

        test("two instances with the same values are equal") {
            val a = ExponentialBackoffStrategyData(60.seconds, 2.0, 1.seconds)
            val b = ExponentialBackoffStrategyData(60.seconds, 2.0, 1.seconds)
            a shouldBe b
        }

        test("two instances with different multipliers are not equal") {
            val a = ExponentialBackoffStrategyData(60.seconds, 2.0, 1.seconds)
            val b = ExponentialBackoffStrategyData(60.seconds, 3.0, 1.seconds)
            a shouldNotBe b
        }

        test("two instances with different initialDelays are not equal") {
            val a = ExponentialBackoffStrategyData(60.seconds, 2.0, 1.seconds)
            val b = ExponentialBackoffStrategyData(60.seconds, 2.0, 2.seconds)
            a shouldNotBe b
        }

        test("two instances with different maxDelays are not equal") {
            val a = ExponentialBackoffStrategyData(30.seconds, 2.0, 1.seconds)
            val b = ExponentialBackoffStrategyData(60.seconds, 2.0, 1.seconds)
            a shouldNotBe b
        }
    }
})
