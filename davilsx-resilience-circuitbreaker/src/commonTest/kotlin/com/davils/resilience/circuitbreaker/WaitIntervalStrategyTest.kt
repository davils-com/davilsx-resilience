package com.davils.resilience.circuitbreaker

import com.davils.resilience.circuitbreaker.strategy.exponentialWaitInterval
import com.davils.resilience.circuitbreaker.strategy.fixedWaitInterval
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WaitIntervalStrategyTest : FunSpec({
    context("fixedWaitInterval") {
        test("always returns the configured duration regardless of attempts") {
            val strategy = fixedWaitInterval(30.seconds)
            strategy.waitDuration(1) shouldBe 30.seconds
            strategy.waitDuration(5) shouldBe 30.seconds
            strategy.waitDuration(100) shouldBe 30.seconds
        }
    }

    context("exponentialWaitInterval") {
        test("first attempt returns initialDuration") {
            val strategy = exponentialWaitInterval(initialDuration = 10.seconds, multiplier = 2.0)
            strategy.waitDuration(1) shouldBe 10.seconds
        }

        test("doubles on each subsequent attempt") {
            val strategy = exponentialWaitInterval(initialDuration = 10.seconds, multiplier = 2.0)
            strategy.waitDuration(2) shouldBe 20.seconds
            strategy.waitDuration(3) shouldBe 40.seconds
        }

        test("is capped by maxDuration") {
            val strategy = exponentialWaitInterval(
                initialDuration = 10.seconds,
                multiplier = 2.0,
                maxDuration = 30.seconds
            )
            strategy.waitDuration(1) shouldBe 10.seconds
            strategy.waitDuration(2) shouldBe 20.seconds
            strategy.waitDuration(3) shouldBe 30.seconds
            strategy.waitDuration(10) shouldBe 30.seconds
        }

        test("multiplier below 1 is rejected") {
            shouldThrow<IllegalArgumentException> {
                exponentialWaitInterval(multiplier = 0.5)
            }
        }
    }
})
