package com.davils.resilience.metrics.circuitbreaker

import com.davils.resilience.circuitbreaker.circuitBreaker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

class CircuitBreakerMetricsCollectorTest : FunSpec({
    test("allMetrics reflects successful and failed calls") {
        val cb = circuitBreaker {
            failureRateThreshold = 50f
            slidingWindowSize = 4
            minimumNumberOfCalls = 4
            waitDurationInOpenState(200.milliseconds)
        }
        cb.execute { "ok" }
        runCatching { cb.execute { throw RuntimeException() } }

        val snapshot = cb.metrics.allMetrics()
        snapshot.numberOfSuccessfulCalls shouldBe 1
        snapshot.numberOfFailedCalls shouldBe 1
        snapshot.numberOfBufferedCalls shouldBe 2
    }

    test("metrics extension provides collector access") {
        val cb = circuitBreaker {
            slidingWindowSize = 10
            minimumNumberOfCalls = 10
        }
        cb.execute { "ok" }

        cb.metrics().allMetrics().numberOfSuccessfulCalls shouldBe 1

        var configured = false
        cb.metrics { configured = true }
        configured shouldBe true
    }
})
