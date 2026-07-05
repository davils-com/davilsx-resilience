package com.davils.resilience.metrics.ratelimiter

import com.davils.resilience.ratelimiter.fixedRateLimiter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RateLimiterMetricsCollectorTest : FunSpec({
    test("allMetrics reflects successful and failed acquisitions") {
        val limiter = fixedRateLimiter(limit = 2, period = 500.milliseconds) {
            timeoutDuration = Duration.ZERO
        }
        limiter.tryAcquire()
        limiter.tryAcquire()
        limiter.tryAcquire()

        val snapshot = limiter.metrics.allMetrics()
        snapshot.numberOfSuccessfulAcquires shouldBe 2
        snapshot.numberOfFailedAcquires shouldBe 1
        snapshot.limitForPeriod shouldBe 2
    }

    test("metrics extension provides collector access") {
        val limiter = fixedRateLimiter(limit = 2, period = 500.milliseconds) {
            timeoutDuration = Duration.ZERO
        }
        limiter.tryAcquire()

        limiter.metrics().allMetrics().numberOfSuccessfulAcquires shouldBe 1

        var configured = false
        limiter.metrics { configured = true }
        configured shouldBe true
    }
})
