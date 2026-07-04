package com.davils.resilience.circuitbreaker

import com.davils.resilience.circuitbreaker.internal.CallOutcome
import com.davils.resilience.circuitbreaker.internal.CountBasedSlidingWindow
import com.davils.resilience.circuitbreaker.internal.TimeBasedSlidingWindow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds

class SlidingWindowMetricsTest : FunSpec({
    context("COUNT_BASED sliding window") {
        test("returns zero snapshot when no calls recorded") {
            val w = CountBasedSlidingWindow(5)
            val s = w.snapshot()
            s.totalCalls shouldBe 0
            s.successCalls shouldBe 0
            s.errorCalls shouldBe 0
        }

        test("records success outcome correctly") {
            val w = CountBasedSlidingWindow(5)
            w.record(CallOutcome.SUCCESS, 10.milliseconds)
            val s = w.snapshot()
            s.totalCalls shouldBe 1
            s.successCalls shouldBe 1
            s.errorCalls shouldBe 0
        }

        test("records error outcome correctly") {
            val w = CountBasedSlidingWindow(5)
            w.record(CallOutcome.ERROR, 10.milliseconds)
            val s = w.snapshot()
            s.totalCalls shouldBe 1
            s.errorCalls shouldBe 1
        }

        test("records slow success and slow error") {
            val w = CountBasedSlidingWindow(5)
            w.record(CallOutcome.SLOW_SUCCESS, 100.milliseconds)
            w.record(CallOutcome.SLOW_ERROR, 200.milliseconds)
            val s = w.snapshot()
            s.slowCalls shouldBe 2
            s.slowSuccessCalls shouldBe 1
            s.slowErrorCalls shouldBe 1
        }

        test("evicts oldest entry when window is full") {
            val w = CountBasedSlidingWindow(3)
            w.record(CallOutcome.ERROR, 1.milliseconds)
            w.record(CallOutcome.SUCCESS, 1.milliseconds)
            w.record(CallOutcome.SUCCESS, 1.milliseconds)
            // window full — evict the oldest ERROR
            w.record(CallOutcome.SUCCESS, 1.milliseconds)
            val s = w.snapshot()
            s.totalCalls shouldBe 3
            s.errorCalls shouldBe 0
            s.successCalls shouldBe 3
        }

        test("failure rate is -1 below minimum calls") {
            val w = CountBasedSlidingWindow(5)
            w.record(CallOutcome.ERROR, 1.milliseconds)
            val s = w.snapshot()
            s.failureRate(3) shouldBe -1f
        }

        test("failure rate returns correct percentage") {
            val w = CountBasedSlidingWindow(4)
            repeat(2) { w.record(CallOutcome.ERROR, 1.milliseconds) }
            repeat(2) { w.record(CallOutcome.SUCCESS, 1.milliseconds) }
            val s = w.snapshot()
            s.failureRate(4) shouldBe 50f
        }

        test("slow call rate returns correct percentage") {
            val w = CountBasedSlidingWindow(4)
            repeat(1) { w.record(CallOutcome.SLOW_SUCCESS, 1.milliseconds) }
            repeat(3) { w.record(CallOutcome.SUCCESS, 1.milliseconds) }
            val s = w.snapshot()
            s.slowCallRate(4) shouldBe 25f
        }

        test("reset clears all data") {
            val w = CountBasedSlidingWindow(5)
            repeat(3) { w.record(CallOutcome.ERROR, 1.milliseconds) }
            w.reset()
            val s = w.snapshot()
            s.totalCalls shouldBe 0
            s.errorCalls shouldBe 0
        }
    }

    context("TIME_BASED sliding window") {
        test("records and snapshots correctly within window") {
            var fakeTime = 0L
            val w = TimeBasedSlidingWindow(windowSeconds = 5, clock = { fakeTime })
            w.record(CallOutcome.SUCCESS, 10.milliseconds)
            w.record(CallOutcome.ERROR, 10.milliseconds)
            val s = w.snapshot()
            s.totalCalls shouldBe 2
            s.successCalls shouldBe 1
            s.errorCalls shouldBe 1
        }

        test("evicts entries older than window seconds") {
            var fakeTime = 0L
            val w = TimeBasedSlidingWindow(windowSeconds = 5, clock = { fakeTime })
            w.record(CallOutcome.ERROR, 10.milliseconds)
            fakeTime = 6_000_000_000L // advance 6 seconds
            w.record(CallOutcome.SUCCESS, 10.milliseconds)
            val s = w.snapshot()
            s.totalCalls shouldBe 1
            s.errorCalls shouldBe 0
            s.successCalls shouldBe 1
        }

        test("reset clears all entries") {
            var fakeTime = 0L
            val w = TimeBasedSlidingWindow(windowSeconds = 5, clock = { fakeTime })
            repeat(3) { w.record(CallOutcome.ERROR, 1.milliseconds) }
            w.reset()
            w.snapshot().totalCalls shouldBe 0
        }
    }
})
