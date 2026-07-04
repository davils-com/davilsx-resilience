package com.davils.resilience.metrics.ratelimiter

import com.davils.resilience.ratelimiter.fixedRateLimiter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RateLimiterMetricsCollectorTest : FunSpec({
    test("scrape captures rate limiter metrics") {
        val limiter = fixedRateLimiter(limit = 2, period = 500.milliseconds) {
            timeoutDuration = Duration.ZERO
        }
        limiter.tryAcquire()
        limiter.tryAcquire()
        limiter.tryAcquire()

        val collector = limiter.metrics()
        val metrics = collector.refresh()
        metrics.numberOfSuccessfulAcquires shouldBe 2
        metrics.numberOfFailedAcquires shouldBe 1
        metrics.limitForPeriod shouldBe 2
    }
})
