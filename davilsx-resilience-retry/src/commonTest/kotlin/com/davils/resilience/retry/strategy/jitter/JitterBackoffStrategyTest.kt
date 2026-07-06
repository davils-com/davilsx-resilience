package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.constant.constantBackoff
import com.davils.resilience.retry.strategy.exponential.exponentialBackoff
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.shouldBe

class JitterBackoffStrategyTest : FunSpec({
    context("Functions") {
        test("calculated delay is within the jittered range around the base delay") {
            val base = constantBackoff { delay(1000L) }
            val strategy = jitterBackoff(base) { factor(0.5) }

            // factor = 0.5 → randomFactor ∈ [0.5, 1.5] → delay ∈ [500ms, 1500ms]
            val delayMs = strategy.calculateDelay(1).inWholeMilliseconds.toDouble()
            delayMs.shouldBeBetween(500.0, 1500.0, 0.0)
        }

        test("calculateDelay uses base strategy's delay for the given attempt") {
            val base = exponentialBackoff {
                initialDelay(1000L)
                multiplier(2.0)
                maxDelay(60000L)
            }
            val strategy = jitterBackoff(base) { factor(0.001) }

            // factor = 0.001 → randomFactor ∈ [0.999, 1.001] → within 0.1% of base delay
            val delay1Ms = strategy.calculateDelay(1).inWholeMilliseconds.toDouble()
            val delay2Ms = strategy.calculateDelay(2).inWholeMilliseconds.toDouble()

            delay1Ms.shouldBeBetween(999.0, 1001.0, 0.0)
            delay2Ms.shouldBeBetween(1998.0, 2002.0, 0.0)
        }

        test("withJitter extension creates a valid jitter strategy") {
            val strategy = constantBackoff { delay(1000L) }.withJitter { factor(0.2) }

            // factor = 0.2 → delay ∈ [800ms, 1200ms]
            val delayMs = strategy.calculateDelay(1).inWholeMilliseconds.toDouble()
            delayMs.shouldBeBetween(800.0, 1200.0, 0.0)
        }

        test("withJitter(factor) extension creates a valid jitter strategy") {
            val strategy = constantBackoff { delay(1000L) }.withJitter(0.3)

            // factor = 0.3 → delay ∈ [700ms, 1300ms]
            val delayMs = strategy.calculateDelay(1).inWholeMilliseconds.toDouble()
            delayMs.shouldBeBetween(700.0, 1300.0, 0.0)
        }

        test("jitterBackoff with default factor uses 0.5") {
            val base = constantBackoff { delay(1000L) }
            val strategy = jitterBackoff(base)

            // default factor = 0.5 → delay ∈ [500ms, 1500ms]
            val delayMs = strategy.calculateDelay(1).inWholeMilliseconds.toDouble()
            delayMs.shouldBeBetween(500.0, 1500.0, 0.0)
        }

        test("delay with factor 1.0 is within full range") {
            val base = constantBackoff { delay(1000L) }
            val strategy = jitterBackoff(base) { factor(1.0) }

            // factor = 1.0 → randomFactor ∈ [0.0, 2.0] → delay ∈ [0ms, 2000ms]
            val delayMs = strategy.calculateDelay(1).inWholeMilliseconds
            (delayMs >= 0) shouldBe true
        }

        test("two successive calls may produce different delays") {
            val base = constantBackoff { delay(1000L) }
            val strategy = jitterBackoff(base) { factor(0.5) }

            val results = (1..50).map { strategy.calculateDelay(1) }.toSet()
            (results.size > 1) shouldBe true
        }
    }
})
