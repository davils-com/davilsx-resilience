package com.davils.resilience.metrics.timelimiter

import com.davils.resilience.timelimiter.hardTimeLimiter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import com.davils.resilience.timelimiter.TimeoutExceededException
import kotlin.time.Duration.Companion.milliseconds

class TimeLimiterMetricsCollectorTest : FunSpec({
    test("allMetrics reflects successful and timed-out executions") {
        val limiter = hardTimeLimiter(100.milliseconds)
        limiter.execute { "ok" }
        shouldThrow<TimeoutExceededException> {
            limiter.execute { delay(200); "late" }
        }

        val snapshot = limiter.metrics.allMetrics()
        snapshot.numberOfSuccessfulCalls shouldBe 1
        snapshot.numberOfTimeoutCalls shouldBe 1
        snapshot.timeout shouldBe 100.milliseconds
    }

    test("metrics extension provides collector access") {
        val limiter = hardTimeLimiter(100.milliseconds)
        limiter.execute { "ok" }

        limiter.metrics().allMetrics().numberOfSuccessfulCalls shouldBe 1

        var configured = false
        limiter.metrics { configured = true }
        configured shouldBe true
    }
})
