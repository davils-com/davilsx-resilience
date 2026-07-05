package com.davils.resilience.ratelimiter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private fun smallLimiter(
    limit: Int = 2,
    periodMs: Long = 200,
    timeoutMs: Long = 100,
    block: RateLimiterBuilder.() -> Unit = {},
) = rateLimiter {
    limitForPeriod = limit
    limitRefreshPeriod = periodMs.milliseconds
    timeoutDuration = timeoutMs.milliseconds
    apply(block)
}

class RateLimiterTest : FunSpec({
    context("fixed window") {
        test("allows calls up to limitForPeriod without waiting") {
            val limiter = smallLimiter(limit = 3, timeoutMs = 0)
            repeat(3) {
                limiter.tryAcquire() shouldBe true
            }
            limiter.getAvailableSlots() shouldBe 0
        }

        test("tryAcquire rejects when limit is exceeded") {
            val limiter = smallLimiter(limit = 2, timeoutMs = 0)
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
        }

        test("execute throws RequestNotPermittedException when rejected") {
            val limiter = smallLimiter(limit = 1, timeoutMs = 0, block = { strategy = RateLimiterStrategy.FAIL_FAST })
            limiter.execute { "ok" } shouldBe "ok"
            shouldThrow<RequestNotPermittedException> {
                limiter.execute { "blocked" }
            }
        }

        test("refills permits after refresh period") {
            val limiter = smallLimiter(limit = 1, periodMs = 100, timeoutMs = 0)
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
            delay(150)
            limiter.tryAcquire() shouldBe true
        }

        test("acquireSlot waits when timeout allows") {
            val limiter = smallLimiter(limit = 1, periodMs = 200, timeoutMs = 500)
            limiter.tryAcquire() shouldBe true
            val acquired = limiter.acquireSlot()
            acquired shouldBe true
        }

        test("acquireSlot rejects when wait exceeds timeout") {
            val limiter = smallLimiter(limit = 1, periodMs = 500, timeoutMs = 50)
            limiter.tryAcquire() shouldBe true
            limiter.acquireSlot() shouldBe false
        }

        test("supports multi-permit acquisition") {
            val limiter = smallLimiter(limit = 5, timeoutMs = 0)
            limiter.tryAcquire(3) shouldBe true
            limiter.getAvailableSlots() shouldBe 2
            limiter.tryAcquire(3) shouldBe false
            limiter.tryAcquire(2) shouldBe true
        }

        test("FAIL_FAST rejects when wait would be required") {
            val limiter = smallLimiter(limit = 1, periodMs = 500, timeoutMs = 5_000, block = {
                strategy = RateLimiterStrategy.FAIL_FAST
            })
            limiter.tryAcquire() shouldBe true
            limiter.acquireSlot() shouldBe false
        }

        test("BLOCKING waits until permit becomes available") {
            val limiter = smallLimiter(limit = 1, periodMs = 100, timeoutMs = 0, block = {
                strategy = RateLimiterStrategy.BLOCKING
            })
            limiter.tryAcquire() shouldBe true
            limiter.acquireSlot() shouldBe true
        }
    }

    context("sliding window") {
        test("limits requests within rolling window") {
            val limiter = slidingWindowRateLimiter(limit = 2, period = 200.milliseconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
        }

        test("releases permits as window slides") {
            val limiter = slidingWindowRateLimiter(limit = 1, period = 100.milliseconds) {
                timeoutDuration = Duration.ZERO
                strategy = RateLimiterStrategy.FAIL_FAST
            }
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
            delay(150)
            limiter.tryAcquire() shouldBe true
        }
    }

    context("runtime configuration") {
        test("changeLimitForPeriod updates available capacity") {
            val limiter = smallLimiter(limit = 1, timeoutMs = 0)
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
            limiter.changeLimitForPeriod(3)
            limiter.getAvailableSlots() shouldBe 2
        }

        test("changeTimeoutDuration affects subsequent acquires") {
            val limiter = smallLimiter(limit = 1, periodMs = 500, timeoutMs = 10)
            limiter.tryAcquire() shouldBe true
            limiter.acquireSlot() shouldBe false
            limiter.changeTimeoutDuration(1.seconds)
            limiter.acquireSlot() shouldBe true
        }
    }

    context("metrics") {
        test("tracks successful and failed acquisitions") {
            val limiter = smallLimiter(limit = 1, timeoutMs = 0)
            limiter.tryAcquire() shouldBe true
            limiter.tryAcquire() shouldBe false
            val metrics = limiter.getMetrics()
            metrics.numberOfSuccessfulAcquires shouldBe 1
            metrics.numberOfFailedAcquires shouldBe 1
            metrics.availablePermissions shouldBe 0
        }
    }

    context("events") {
        test("emits SuccessfulAcquire and FailedAcquire") {
            val limiter = smallLimiter(limit = 1, timeoutMs = 0)
            val events = Channel<RateLimiterEvent>(4)
            limiter.subscribe<RateLimiterEvent> { events.send(it) }
            delay(10)

            limiter.tryAcquire()
            limiter.tryAcquire()

            withTimeout(500.milliseconds) {
                events.receive().shouldBeInstanceOf<RateLimiterEvent.SuccessfulAcquire>()
                events.receive().shouldBeInstanceOf<RateLimiterEvent.FailedAcquire>()
            }
        }

        test("FailedAcquire contains computed wait duration") {
            val limiter = smallLimiter(limit = 1, periodMs = 500, timeoutMs = 10)
            val events = Channel<RateLimiterEvent.FailedAcquire>(2)
            limiter.subscribe<RateLimiterEvent.FailedAcquire> { events.send(it) }
            delay(10)

            limiter.tryAcquire()
            limiter.acquireSlot()

            withTimeout(500.milliseconds) {
                val failed = events.receive()
                (failed.waitDuration > 10.milliseconds) shouldBe true
            }
        }
    }

    context("disposal") {
        test("throws CancellationException after dispose") {
            val limiter = smallLimiter()
            limiter.dispose()
            shouldThrow<CancellationException> {
                limiter.tryAcquire()
            }
        }
    }

    context("concurrency") {
        test("does not over-allocate permits under parallel load") {
            val limiter = smallLimiter(limit = 5, periodMs = 1_000, timeoutMs = 0)
            coroutineScope {
                val results = (1..20).map {
                    async { limiter.tryAcquire() }
                }.map { it.await() }
                results.count { it } shouldBe 5
            }
        }
    }
})
