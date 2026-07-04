package com.davils.resilience.circuitbreaker

import com.davils.resilience.circuitbreaker.exception.CallNotPermittedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private fun smallCb(
    failureRate: Float = 50f,
    windowSize: Int = 4,
    minCalls: Int = 4,
    waitMs: Long = 200,
    block: CircuitBreakerBuilder.() -> Unit = {},
) = circuitBreaker {
    failureRateThreshold = failureRate
    slidingWindowSize = windowSize
    minimumNumberOfCalls = minCalls
    waitDurationInOpenState(waitMs.milliseconds)
    permittedCallsInHalfOpenState = 2
    apply(block)
}

class CircuitBreakerTest : FunSpec({
    context("CLOSED state") {
        test("allows calls through when below threshold") {
            val cb = smallCb()
            var ran = false
            cb.execute { ran = true }
            ran shouldBe true
        }

        test("starts in CLOSED state") {
            val cb = smallCb()
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }

        test("transitions to OPEN when failure rate reaches threshold") {
            val cb = smallCb(failureRate = 50f, windowSize = 4, minCalls = 4)
            repeat(2) { runCatching { cb.execute { throw RuntimeException("fail") } } }
            repeat(2) { runCatching { cb.execute { "ok" } } }
            // 50% failure rate >= 50% threshold → open
            cb.getState() shouldBe CircuitBreakerState.OPEN
        }

        test("does not open when failure rate is below threshold") {
            val cb = smallCb(failureRate = 51f, windowSize = 4, minCalls = 4)
            repeat(2) { runCatching { cb.execute { throw RuntimeException("fail") } } }
            repeat(2) { runCatching { cb.execute { "ok" } } }
            // 50% < 51% — should remain closed
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }

        test("does not open before minimumNumberOfCalls is reached") {
            val cb = smallCb(failureRate = 1f, windowSize = 4, minCalls = 4)
            repeat(3) { runCatching { cb.execute { throw RuntimeException("fail") } } }
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }
    }

    context("OPEN state") {
        test("rejects all calls with CallNotPermittedException") {
            val cb = smallCb(failureRate = 49f, windowSize = 4, minCalls = 4)
            repeat(4) { runCatching { cb.execute { throw RuntimeException("fail") } } }
            cb.getState() shouldBe CircuitBreakerState.OPEN
            shouldThrow<CallNotPermittedException> { cb.execute { "should not run" } }
        }

        test("transitions to HALF_OPEN after wait duration on next call") {
            val cb = smallCb(failureRate = 49f, windowSize = 4, minCalls = 4, waitMs = 100)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            delay(150)
            // Next call attempt should flip to HALF_OPEN
            cb.tryAcquirePermission()
            cb.getState() shouldBe CircuitBreakerState.HALF_OPEN
        }
    }

    context("HALF_OPEN state") {
        suspend fun openThenWait(cb: CircuitBreaker) {
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            delay(250)
            cb.tryAcquirePermission() // trigger OPEN→HALF_OPEN
        }

        test("allows only permittedCallsInHalfOpenState probe calls") {
            val cb = smallCb(failureRate = 49f, waitMs = 100)
            openThenWait(cb)
            // openThenWait consumed 1 of the 2 permits during the OPEN→HALF_OPEN transition.
            val p1 = cb.tryAcquirePermission()  // last remaining permit
            val p2 = cb.tryAcquirePermission()  // quota exhausted
            p1 shouldBe true
            p2 shouldBe false
        }

        test("transitions to CLOSED when all probe calls succeed") {
            val cb = smallCb(failureRate = 49f, waitMs = 100)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            delay(150)
            repeat(2) { cb.execute { "probe" } }
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }

        test("transitions back to OPEN when probe calls exceed threshold") {
            val cb = smallCb(failureRate = 49f, waitMs = 100)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            delay(150)
            // Both probe calls fail → should reopen
            repeat(2) { runCatching { cb.execute { throw RuntimeException() } } }
            cb.getState() shouldBe CircuitBreakerState.OPEN
        }
    }

    context("manual transitions") {
        test("transitionToOpen moves from CLOSED to OPEN") {
            val cb = smallCb()
            cb.transitionToOpen()
            cb.getState() shouldBe CircuitBreakerState.OPEN
        }

        test("transitionToDisabled allows calls regardless of failures") {
            val cb = smallCb()
            cb.transitionToDisabled()
            cb.getState() shouldBe CircuitBreakerState.DISABLED
            var ran = false
            cb.execute { ran = true }
            ran shouldBe true
        }

        test("transitionToForcedOpen rejects all calls") {
            val cb = smallCb()
            cb.transitionToForcedOpen()
            cb.getState() shouldBe CircuitBreakerState.FORCED_OPEN
            shouldThrow<CallNotPermittedException> { cb.execute { "nope" } }
        }

        test("transitionToMetricsOnly allows calls and records metrics") {
            val cb = smallCb()
            cb.transitionToMetricsOnly()
            cb.getState() shouldBe CircuitBreakerState.METRICS_ONLY
            cb.execute { "ok" }
            // Should still be in METRICS_ONLY — thresholds not enforced
            cb.getState() shouldBe CircuitBreakerState.METRICS_ONLY
        }

        test("reset transitions back to CLOSED and clears state") {
            val cb = smallCb(failureRate = 49f, waitMs = 100)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            cb.getState() shouldBe CircuitBreakerState.OPEN
            cb.reset()
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }
    }

    context("exception predicates") {
        test("ignored exceptions are not counted as failures") {
            val cb = circuitBreaker {
                failureRateThreshold = 49f
                slidingWindowSize = 4
                minimumNumberOfCalls = 4
                waitDurationInOpenState(1.seconds)
                ignoreException { on<IllegalArgumentException>() }
            }
            repeat(4) { runCatching { cb.execute { throw IllegalArgumentException("ignored") } } }
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }

        test("only specified exceptions are recorded as failures") {
            val cb = circuitBreaker {
                failureRateThreshold = 49f
                slidingWindowSize = 4
                minimumNumberOfCalls = 4
                waitDurationInOpenState(1.seconds)
                // Record only IllegalStateException; throw IllegalArgumentException (a sibling, not a subclass)
                recordException { on<IllegalStateException>() }
            }
            repeat(4) { runCatching { cb.execute { throw IllegalArgumentException("not recorded") } } }
            // IllegalArgumentException is not a subclass of IllegalStateException → treated as success → stays CLOSED
            cb.getState() shouldBe CircuitBreakerState.CLOSED
        }
    }

    context("result predicate") {
        test("result matching recordResult predicate counts as failure") {
            val cb = circuitBreaker {
                failureRateThreshold = 49f
                slidingWindowSize = 4
                minimumNumberOfCalls = 4
                waitDurationInOpenState(1.seconds)
                recordResult { result -> result == "FAILURE_RESULT" }
            }
            // Explicit type annotation prevents Kotlin from inferring T=Unit due to the Unit
            // lambda context of `repeat`, which would otherwise cause the block result to be
            // discarded and onResult to receive Unit instead of "FAILURE_RESULT".
            repeat(4) { cb.execute<String> { "FAILURE_RESULT" } }
            val m = cb.getMetrics()
            m.numberOfBufferedCalls shouldBe 4
            m.numberOfFailedCalls shouldBe 4
            cb.getState() shouldBe CircuitBreakerState.OPEN
        }
    }

    context("events") {
        test("emits StateTransition event on CLOSED to OPEN") {
            val cb = smallCb(failureRate = 49f, waitMs = 1000)
            val events = Channel<CircuitBreakerEvent>(Channel.UNLIMITED)
            cb.subscribe<CircuitBreakerEvent> { events.send(it) }
            // Allow the subscriber coroutine to start before emitting events.
            delay(10)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }

            withTimeout(500.milliseconds) {
                var transition: CircuitBreakerEvent.StateTransition? = null
                while (transition == null) {
                    transition = events.receive() as? CircuitBreakerEvent.StateTransition
                }
                transition.from shouldBe CircuitBreakerState.CLOSED
                transition.to shouldBe CircuitBreakerState.OPEN
            }
        }

        test("emits CallNotPermitted when OPEN") {
            val cb = smallCb(failureRate = 49f, waitMs = 1000)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            val events = Channel<CircuitBreakerEvent>(Channel.UNLIMITED)
            cb.subscribe<CircuitBreakerEvent> { events.send(it) }
            delay(10)
            runCatching { cb.execute { "blocked" } }
            withTimeout(300.milliseconds) {
                while (true) {
                    val e = events.receive()
                    if (e is CircuitBreakerEvent.CallNotPermitted) break
                }
            }
        }

        test("emits Reset event on reset()") {
            val cb = smallCb()
            val events = Channel<CircuitBreakerEvent>(Channel.UNLIMITED)
            cb.subscribe<CircuitBreakerEvent> { events.send(it) }
            delay(10)
            cb.reset()
            withTimeout(300.milliseconds) {
                while (true) {
                    val e = events.receive()
                    if (e is CircuitBreakerEvent.Reset) break
                }
            }
        }
    }

    context("metrics") {
        test("tracks successful and failed calls") {
            val cb = smallCb()
            cb.execute { "ok" }
            runCatching { cb.execute { throw RuntimeException() } }
            val m = cb.getMetrics()
            m.numberOfSuccessfulCalls shouldBe 1
            m.numberOfFailedCalls shouldBe 1
            m.numberOfBufferedCalls shouldBe 2
        }

        test("tracks not-permitted calls when OPEN") {
            val cb = smallCb(failureRate = 49f, waitMs = 1000)
            repeat(4) { runCatching { cb.execute { throw RuntimeException() } } }
            runCatching { cb.execute { "blocked" } }
            val m = cb.getMetrics()
            m.numberOfNotPermittedCalls shouldBe 1
        }
    }

    context("initialState") {
        test("circuit breaker can start in OPEN state") {
            val cb = circuitBreaker {
                initialState = CircuitBreakerState.OPEN
                waitDurationInOpenState(5.seconds)
            }
            cb.getState() shouldBe CircuitBreakerState.OPEN
            shouldThrow<CallNotPermittedException> { cb.execute { "blocked" } }
        }

        test("circuit breaker can start in DISABLED state") {
            val cb = circuitBreaker { initialState = CircuitBreakerState.DISABLED }
            cb.getState() shouldBe CircuitBreakerState.DISABLED
        }
    }
})
