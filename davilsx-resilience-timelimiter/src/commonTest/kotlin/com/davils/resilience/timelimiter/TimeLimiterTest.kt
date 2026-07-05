package com.davils.resilience.timelimiter

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

private fun hardLimiter(
    timeoutMs: Long = 100,
    block: TimeLimiterBuilder.() -> Unit = {},
) = hardTimeLimiter(timeoutMs.milliseconds, block)

private fun softLimiter(
    timeoutMs: Long = 100,
    block: TimeLimiterBuilder.() -> Unit = {},
) = softTimeLimiter(timeoutMs.milliseconds, block)

class TimeLimiterTest : FunSpec({
    context("hard strategy") {
        test("completes within timeout") {
            val limiter = hardLimiter(timeoutMs = 500)
            limiter.execute { "ok" } shouldBe "ok"
        }

        test("throws TimeoutExceededException when exceeded") {
            val limiter = hardLimiter(timeoutMs = 50)
            shouldThrow<TimeoutExceededException> {
                limiter.execute {
                    delay(200)
                    "late"
                }
            }
        }

        test("fallback returns substitute value on timeout") {
            val limiter = hardLimiter(timeoutMs = 50) {
                fallback<String> { "fallback" }
            }
            limiter.execute {
                delay(200)
                "late"
            } shouldBe "fallback"
        }

        test("fallback rethrow propagates") {
            val limiter = hardLimiter(timeoutMs = 50) {
                fallback<String> { throw IllegalStateException("fallback failed") }
            }
            shouldThrow<IllegalStateException> {
                limiter.execute {
                    delay(200)
                    "late"
                }
            }
        }
    }

    context("soft strategy") {
        test("returns result when block completes in time") {
            val limiter = softLimiter(timeoutMs = 500)
            limiter.execute {
                delay(50)
                "ok"
            } shouldBe "ok"
        }

        test("times out and cancels background work when cancelOnTimeout is true") {
            val limiter = softLimiter(timeoutMs = 50) {
                cancelOnTimeout = true
            }
            shouldThrow<TimeoutExceededException> {
                limiter.execute {
                    delay(500)
                    "late"
                }
            }
        }

        test("times out without cancelling background work when cancelOnTimeout is false") {
            val limiter = softLimiter(timeoutMs = 50) {
                cancelOnTimeout = false
            }
            shouldThrow<TimeoutExceededException> {
                limiter.execute {
                    delay(500)
                    "late"
                }
            }
        }
    }

    context("zero timeout") {
        test("bypasses limiting") {
            val limiter = hardLimiter {
                timeout = Duration.ZERO
            }
            limiter.execute {
                delay(200)
                "ok"
            } shouldBe "ok"
        }
    }

    context("events") {
        test("emits TimeoutExceeded for hard strategy") {
            val limiter = hardLimiter(timeoutMs = 50)
            val events = Channel<TimeLimiterEvent>(1)
            limiter.subscribe<TimeLimiterEvent.TimeoutExceeded> { events.send(it) }

            shouldThrow<TimeoutExceededException> {
                limiter.execute { delay(200) }
            }

            withTimeout(500.milliseconds) {
                val event = events.receive()
                event.shouldBeInstanceOf<TimeLimiterEvent.TimeoutExceeded>()
                event.timeoutMs shouldBe 50
            }
        }

        test("emits TimeoutExceeded for soft strategy") {
            val limiter = softLimiter(timeoutMs = 50)
            val events = Channel<TimeLimiterEvent>(1)
            limiter.subscribe<TimeLimiterEvent.TimeoutExceeded> { events.send(it) }

            shouldThrow<TimeoutExceededException> {
                limiter.execute { delay(200) }
            }

            withTimeout(500.milliseconds) {
                val event = events.receive()
                event.shouldBeInstanceOf<TimeLimiterEvent.TimeoutExceeded>()
            }
        }
    }

    context("metrics") {
        test("increments success and timeout counters") {
            val limiter = hardLimiter(timeoutMs = 100)
            limiter.execute { "ok" }
            shouldThrow<TimeoutExceededException> {
                limiter.execute { delay(200) }
            }

            val metrics = limiter.getMetrics()
            metrics.numberOfSuccessfulCalls shouldBe 1
            metrics.numberOfTimeoutCalls shouldBe 1
            metrics.timeout shouldBe 100.milliseconds
            metrics.strategy shouldBe TimeoutStrategy.HARD
        }
    }

    context("runtime configuration") {
        test("changeTimeout affects subsequent calls") {
            val limiter = hardLimiter(timeoutMs = 50)
            limiter.changeTimeout(500.milliseconds)
            limiter.execute {
                delay(100)
                "ok"
            } shouldBe "ok"
        }
    }

    context("disposal") {
        test("throws CancellationException after dispose") {
            val limiter = hardLimiter()
            limiter.dispose()
            shouldThrow<CancellationException> {
                limiter.execute { "ok" }
            }
        }
    }

    context("concurrency") {
        test("parallel executes respect independent timeouts") {
            val limiter = hardLimiter(timeoutMs = 200)
            coroutineScope {
                val fast = async { limiter.execute { "fast" } }
                val slow = async {
                    runCatching { limiter.execute { delay(500); "slow" } }
                }
                fast.await() shouldBe "fast"
                slow.await().isFailure shouldBe true
            }
        }
    }
})
