package com.davils.resilience.ratelimiter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RateLimiterBuilderTest : FunSpec({
    context("defaults") {
        test("limitForPeriod defaults to 50") {
            RateLimiterBuilder().limitForPeriod shouldBe 50
        }

        test("limitRefreshPeriod defaults to 500 milliseconds") {
            RateLimiterBuilder().limitRefreshPeriod shouldBe 500.milliseconds
        }

        test("timeoutDuration defaults to 5 seconds") {
            RateLimiterBuilder().timeoutDuration shouldBe 5.seconds
        }

        test("strategy defaults to WAIT") {
            RateLimiterBuilder().strategy shouldBe RateLimiterStrategy.WAIT
        }

        test("windowType defaults to FIXED") {
            RateLimiterBuilder().windowType shouldBe RateLimiterWindowType.FIXED
        }
    }

    context("validation") {
        fun build(block: RateLimiterBuilder.() -> Unit): Result<RateLimiterData> = runCatching {
            RateLimiterBuilder().apply(block).produce()
        }

        test("rejects limitForPeriod below 1") {
            build { limitForPeriod = 0 }.isFailure shouldBe true
        }

        test("rejects non-positive limitRefreshPeriod") {
            build { limitRefreshPeriod = Duration.ZERO }.isFailure shouldBe true
        }

        test("rejects negative timeoutDuration") {
            build { timeoutDuration = (-1).seconds }.isFailure shouldBe true
        }
    }
})
