package com.davils.resilience.retry.strategy.constant

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConstantBackoffStrategyDataTest : FunSpec({
    context("Variables") {
        test("stores the given delay correctly") {
            val data = ConstantBackoffStrategyData(250.milliseconds)
            data.delay shouldBe 250.milliseconds
        }

        test("stores zero delay") {
            val data = ConstantBackoffStrategyData(Duration.ZERO)
            data.delay shouldBe Duration.ZERO
        }

        test("two instances with the same delay are equal") {
            val a = ConstantBackoffStrategyData(1.seconds)
            val b = ConstantBackoffStrategyData(1.seconds)
            a shouldBe b
        }

        test("two instances with different delays are not equal") {
            val a = ConstantBackoffStrategyData(1.seconds)
            val b = ConstantBackoffStrategyData(2.seconds)
            a shouldNotBe b
        }
    }
})
