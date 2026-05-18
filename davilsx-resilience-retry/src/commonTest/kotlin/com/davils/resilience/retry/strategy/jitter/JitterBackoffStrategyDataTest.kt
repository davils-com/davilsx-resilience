package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.constant.constantBackoff
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class JitterBackoffStrategyDataTest : FunSpec({
    context("Variables") {
        test("stores backoffStrategy correctly") {
            val base = constantBackoff { delay(500L) }
            val data = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.5,
                mode = JitterMode.PROPORTIONAL,
                cap = 10.seconds
            )
            data.backoffStrategy shouldBe base
        }

        test("stores factor correctly") {
            val base = constantBackoff()
            val data = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.3,
                mode = JitterMode.PROPORTIONAL,
                cap = 10.seconds
            )
            data.factor shouldBe 0.3
        }

        test("two instances with the same values are equal") {
            val base = constantBackoff { delay(1000L) }
            val a = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.5,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            val b = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.5,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            a shouldBe b
        }

        test("two instances with different factors are not equal") {
            val base = constantBackoff()
            val a = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.3,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            val b = JitterBackoffStrategyData(
                backoffStrategy = base, factor = 0.7,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            a shouldNotBe b
        }

        test("two instances with different backoffStrategies are not equal") {
            val base1 = constantBackoff { delay(500L) }
            val base2 = constantBackoff { delay(1000L) }
            val a = JitterBackoffStrategyData(
                backoffStrategy = base1, factor = 0.5,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            val b = JitterBackoffStrategyData(
                backoffStrategy = base2, factor = 0.5,
                mode = JitterMode.PROPORTIONAL,
                cap = Duration.INFINITE
            )
            a shouldNotBe b
        }
    }
})