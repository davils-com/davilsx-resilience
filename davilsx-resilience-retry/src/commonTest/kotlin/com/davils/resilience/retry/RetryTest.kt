package com.davils.resilience.retry

import com.davils.resilience.retry.event.RetryEvent
import com.davils.resilience.retry.predicate.result.resultPredicate
import com.davils.resilience.retry.predicate.throwable.throwablePredicate
import com.davils.resilience.retry.strategy.constant.constantBackoff
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

private fun fastRetry(block: RetryBuilder.() -> Unit = {}) = retry {
    backoffStrategy(constantBackoff { delay(0L) })
    apply(block)
}

class RetryTest : FunSpec({
    context("throwable retry") {
        test("succeeds on first attempt") {
            val retry = fastRetry { maxAttempts = 3 }
            retry.execute { "ok" } shouldBe "ok"
        }

        test("retries until success") {
            val retry = fastRetry { maxAttempts = 3 }
            var calls = 0
            retry.execute {
                calls++
                if (calls < 3) throw RuntimeException("transient")
                "ok"
            } shouldBe "ok"
            calls shouldBe 3
        }

        test("rethrows after max attempts are exhausted") {
            val retry = fastRetry { maxAttempts = 2 }
            shouldThrow<RuntimeException> {
                retry.execute { throw RuntimeException("fail") }
            }
        }

        test("maxAttempts of 1 performs a single attempt") {
            val retry = fastRetry { maxAttempts = 1 }
            var calls = 0
            shouldThrow<IllegalStateException> {
                retry.execute {
                    calls++
                    throw IllegalStateException("no retry")
                }
            }
            calls shouldBe 1
        }

        test("does not retry non-matching throwables") {
            val retry = fastRetry {
                maxAttempts = 3
                predicate = throwablePredicate { throwable(IllegalStateException::class) }
            }
            var calls = 0
            shouldThrow<RuntimeException> {
                retry.execute {
                    calls++
                    throw RuntimeException("not retryable")
                }
            }
            calls shouldBe 1
        }
    }

    context("result retry") {
        test("retries when result predicate matches") {
            val retry = fastRetry {
                maxAttempts = 3
                predicate = resultPredicate<String> {
                    retryIf { it == "retry" }
                }
            }
            var calls = 0
            retry.execute {
                calls++
                if (calls < 3) "retry" else "done"
            } shouldBe "done"
            calls shouldBe 3
        }

        test("throws MaxRetriesExceededException when configured to throw") {
            val retry = fastRetry {
                maxAttempts = 2
                onResultExhaustion = OnResultExhaustion.THROW
                predicate = resultPredicate<String> {
                    retryIf { it == "bad" }
                }
            }
            val ex = shouldThrow<MaxRetriesExceededException> {
                retry.execute { "bad" }
            }
            ex.attempts shouldBe 2
            ex.lastResult shouldBe "bad"
        }

        test("returns last result when configured with RETURN_LAST") {
            val retry = fastRetry {
                maxAttempts = 2
                onResultExhaustion = OnResultExhaustion.RETURN_LAST
                predicate = resultPredicate<String> {
                    retryIf { it == "bad" }
                }
            }
            retry.execute { "bad" } shouldBe "bad"
        }
    }

    context("failAfterMaxRetries") {
        test("retries indefinitely when disabled") {
            val retry = fastRetry {
                maxAttempts = 2
                failAfterMaxRetries = false
            }
            var calls = 0
            retry.execute {
                calls++
                if (calls < 5) throw RuntimeException("keep going")
                "done"
            } shouldBe "done"
            calls shouldBe 5
        }
    }

    context("cancellation and disposal") {
        test("does not retry CancellationException") {
            val retry = fastRetry { maxAttempts = 3 }
            var calls = 0
            shouldThrow<CancellationException> {
                retry.execute {
                    calls++
                    throw CancellationException("cancelled")
                }
            }
            calls shouldBe 1
        }

        test("execute fails after disposal") {
            val retry = fastRetry()
            retry.dispose()
            shouldThrow<CancellationException> {
                retry.execute { "blocked" }
            }
        }
    }

    context("events") {
        test("emits lifecycle events on success after retries") {
            val retry = fastRetry { maxAttempts = 3 }
            val events = Channel<RetryEvent>(Channel.UNLIMITED)
            retry.subscribe<RetryEvent> { events.send(it) }
            delay(10)

            var calls = 0
            retry.execute {
                calls++
                if (calls < 2) throw RuntimeException("transient")
                "ok"
            }

            withTimeout(500.milliseconds) {
                val received = mutableListOf<RetryEvent>()
                while (
                    received.none { it is RetryEvent.RetrySucceeded } ||
                    received.filterIsInstance<RetryEvent.RetryAttemptStarted>().size < 2
                ) {
                    received += events.receive()
                }
                while (true) {
                    val pending = events.tryReceive().getOrNull() ?: break
                    received += pending
                }
                received.filterIsInstance<RetryEvent.RetryAttemptStarted>().size shouldBe 2
                received.any { it is RetryEvent.RetryAttemptFailed } shouldBe true
                received.any { it is RetryEvent.RetryAttemptBackoff } shouldBe true
            }
        }

        test("emits RetryFailed on permanent failure") {
            val retry = fastRetry { maxAttempts = 1 }
            val events = Channel<RetryEvent>(Channel.UNLIMITED)
            retry.subscribe<RetryEvent> { events.send(it) }
            delay(10)

            runCatching { retry.execute { throw RuntimeException("fail") } }

            withTimeout(500.milliseconds) {
                while (true) {
                    val event = events.receive()
                    if (event is RetryEvent.RetryFailed) {
                        event.throwable.shouldBeInstanceOf<RuntimeException>()
                        break
                    }
                }
            }
        }
    }

    context("metrics") {
        test("getMetrics tracks successful and exhausted calls") {
            val retry = fastRetry { maxAttempts = 2 }
            retry.execute { "ok" }
            runCatching { retry.execute { throw RuntimeException("fail") } }

            val metrics = retry.getMetrics()
            metrics.totalCalls shouldBe 2
            metrics.successfulCalls shouldBe 1
            metrics.exhaustedCalls shouldBe 1
            metrics.totalAttempts shouldBe 3
        }
    }

    context("convenience factories") {
        test("fixedDelayRetry applies constant backoff") {
            val policy = fixedDelayRetry(maxAttempts = 2) {
                backoffStrategy(constantBackoff { delay(0L) })
            }
            policy.execute { "ok" } shouldBe "ok"
        }

        test("exponentialRetry creates a working policy") {
            val policy = exponentialRetry(maxAttempts = 2) {
                backoffStrategy(constantBackoff { delay(0L) })
            }
            policy.execute { "ok" } shouldBe "ok"
        }
    }
})
