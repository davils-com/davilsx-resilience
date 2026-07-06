package com.davils.resilience.metrics.retry

import com.davils.resilience.retry.fixedDelayRetry
import com.davils.resilience.retry.strategy.constant.constantBackoff
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RetryMetricsCollectorTest : FunSpec({
    test("allMetrics reflects successful and exhausted calls") {
        val policy = fixedDelayRetry(maxAttempts = 2) {
            backoffStrategy(constantBackoff { delay(0L) })
        }
        policy.execute { "ok" }
        runCatching { policy.execute { throw RuntimeException("fail") } }

        val snapshot = policy.metrics.allMetrics()
        snapshot.callMetrics.totalCalls shouldBe 2
        snapshot.callMetrics.successfulCalls shouldBe 1
        snapshot.callMetrics.exhaustedCalls shouldBe 1
        snapshot.tryMetrics.totalAttempts shouldBe 3
    }

    test("metrics extension provides collector access") {
        val policy = fixedDelayRetry(maxAttempts = 3) {
            backoffStrategy(constantBackoff { delay(0L) })
        }
        policy.execute { "ok" }

        policy.metrics().allMetrics().callMetrics.successfulCalls shouldBe 1

        var configured = false
        policy.metrics { configured = true }
        configured shouldBe true
    }
})
