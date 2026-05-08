package com.davils.resilience.retry.strategy.exponential

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

class ExponentialBackoffStrategyTest : FunSpec({
    context("Functions") {
        test("attempt 1 returns initialDelay") {
            val strategy = exponentialBackoff {
                initialDelay(1000L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(1) shouldBe 1000.milliseconds
        }

        test("attempt 2 returns initialDelay * multiplier^1") {
            val strategy = exponentialBackoff {
                initialDelay(1000L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(2) shouldBe 2000.milliseconds
        }

        test("attempt 3 returns initialDelay * multiplier^2") {
            val strategy = exponentialBackoff {
                initialDelay(1000L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(3) shouldBe 4000.milliseconds
        }

        test("delay is capped at maxDelay") {
            val strategy = exponentialBackoff {
                initialDelay(1000L)
                multiplier(2.0)
                maxDelay(3000L)
            }
            strategy.calculateDelay(10) shouldBe 3000.milliseconds
        }

        test("attempt 0 is treated as attempt 1") {
            val strategy = exponentialBackoff {
                initialDelay(500L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(0) shouldBe strategy.calculateDelay(1)
        }

        test("negative attempt is treated as attempt 1") {
            val strategy = exponentialBackoff {
                initialDelay(500L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(-5) shouldBe 500.milliseconds
        }

        test("multiplier of 1.0 always returns initialDelay") {
            val strategy = exponentialBackoff {
                initialDelay(1000L)
                multiplier(1.0)
                maxDelay(60000L)
            }
            strategy.calculateDelay(1) shouldBe 1000.milliseconds
            strategy.calculateDelay(5) shouldBe 1000.milliseconds
            strategy.calculateDelay(100) shouldBe 1000.milliseconds
        }

        test("maxDelay equal to initialDelay always returns initialDelay") {
            val strategy = exponentialBackoff {
                initialDelay(500L)
                multiplier(2.0)
                maxDelay(500L)
            }
            strategy.calculateDelay(1) shouldBe 500.milliseconds
            strategy.calculateDelay(5) shouldBe 500.milliseconds
        }

        test("default configuration calculates delay correctly for attempt 1") {
            val strategy = exponentialBackoff()
            strategy.calculateDelay(1) shouldBe 1000.milliseconds
        }

        test("default configuration calculates delay correctly for attempt 2") {
            val strategy = exponentialBackoff()
            strategy.calculateDelay(2) shouldBe 2000.milliseconds
        }
    }
})
