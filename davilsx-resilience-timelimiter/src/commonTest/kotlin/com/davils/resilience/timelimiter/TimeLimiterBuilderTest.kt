package com.davils.resilience.timelimiter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TimeLimiterBuilderTest : FunSpec({
    context("defaults") {
        test("timeout defaults to 1 second") {
            TimeLimiterBuilder().timeout shouldBe 1.seconds
        }

        test("cancelOnTimeout defaults to true") {
            TimeLimiterBuilder().cancelOnTimeout shouldBe true
        }

        test("strategy defaults to HARD") {
            TimeLimiterBuilder().strategy shouldBe TimeoutStrategy.HARD
        }
    }

    context("validation") {
        fun build(block: TimeLimiterBuilder.() -> Unit): Result<TimeLimiterData> = runCatching {
            TimeLimiterBuilder().apply(block).produce()
        }

        test("rejects negative timeout") {
            build { timeout = (-1).seconds }.isFailure shouldBe true
        }

        test("accepts zero timeout") {
            build { timeout = Duration.ZERO }.isSuccess shouldBe true
        }
    }
})
