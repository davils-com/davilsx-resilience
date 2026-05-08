package com.davils.resilience.retry.strategy.constant

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

class ConstantBackoffStrategyTest : FunSpec({
    context("Functions") {
        test("returns the configured delay regardless of attempt number") {
            val strategy = constantBackoff { delay(500L) }
            strategy.calculateDelay(1) shouldBe 500.milliseconds
        }

        test("returns the same delay for attempt 0") {
            val strategy = constantBackoff { delay(200L) }
            strategy.calculateDelay(0) shouldBe 200.milliseconds
        }

        test("returns the same delay for a high attempt number") {
            val strategy = constantBackoff { delay(300L) }
            strategy.calculateDelay(100) shouldBe 300.milliseconds
        }

        test("returns default delay of 1000ms when no builder config is provided") {
            val strategy = constantBackoff()
            strategy.calculateDelay(1) shouldBe 1000.milliseconds
        }
    }
})
