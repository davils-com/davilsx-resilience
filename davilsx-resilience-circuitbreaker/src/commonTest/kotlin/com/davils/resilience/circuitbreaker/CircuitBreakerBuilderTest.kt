package com.davils.resilience.circuitbreaker

import com.davils.resilience.circuitbreaker.strategy.fixedWaitInterval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CircuitBreakerBuilderTest : FunSpec({
    context("defaults") {
        test("failure rate threshold defaults to 50") {
            CircuitBreakerBuilder().failureRateThreshold shouldBe 50f
        }

        test("slow call rate threshold defaults to 100") {
            CircuitBreakerBuilder().slowCallRateThreshold shouldBe 100f
        }

        test("slow call duration threshold defaults to 60 seconds") {
            CircuitBreakerBuilder().slowCallDurationThreshold shouldBe 60.seconds
        }

        test("permitted calls in half open state defaults to 10") {
            CircuitBreakerBuilder().permittedCallsInHalfOpenState shouldBe 10
        }

        test("minimum number of calls defaults to 100") {
            CircuitBreakerBuilder().minimumNumberOfCalls shouldBe 100
        }

        test("sliding window size defaults to 100") {
            CircuitBreakerBuilder().slidingWindowSize shouldBe 100
        }

        test("sliding window type defaults to COUNT_BASED") {
            CircuitBreakerBuilder().slidingWindowType shouldBe SlidingWindowType.COUNT_BASED
        }

        test("automatic transition from open to half open defaults to false") {
            CircuitBreakerBuilder().automaticTransitionFromOpenToHalfOpen shouldBe false
        }

        test("max wait duration in half open state defaults to zero") {
            CircuitBreakerBuilder().maxWaitDurationInHalfOpenState shouldBe Duration.ZERO
        }

        test("transition state after wait duration defaults to OPEN") {
            CircuitBreakerBuilder().transitionStateAfterWaitDuration shouldBe CircuitBreakerState.OPEN
        }

        test("initial state defaults to CLOSED") {
            CircuitBreakerBuilder().initialState shouldBe CircuitBreakerState.CLOSED
        }
    }

    context("setters") {
        fun build(block: CircuitBreakerBuilder.() -> Unit): CircuitBreakerData {
            val b = CircuitBreakerBuilder()
            b.apply(block)
            return b.produce()
        }

        test("failureRateThreshold can be overridden") {
            build { failureRateThreshold = 30f }.failureRateThreshold shouldBe 30f
        }

        test("slowCallRateThreshold can be overridden") {
            build { slowCallRateThreshold = 75f }.slowCallRateThreshold shouldBe 75f
        }

        test("slidingWindowSize can be overridden") {
            build { slidingWindowSize = 20; minimumNumberOfCalls = 5 }.slidingWindowSize shouldBe 20
        }

        test("slidingWindowType can be set to TIME_BASED") {
            build { slidingWindowType = SlidingWindowType.TIME_BASED }.slidingWindowType shouldBe SlidingWindowType.TIME_BASED
        }

        test("permittedCallsInHalfOpenState can be overridden") {
            build { permittedCallsInHalfOpenState = 3 }.permittedCallsInHalfOpenState shouldBe 3
        }

        test("waitDurationInOpenState convenience setter creates fixed wait") {
            val data = build { waitDurationInOpenState(10.seconds) }
            data.waitIntervalInOpenState.waitDuration(1) shouldBe 10.seconds
        }

        test("initialState can be set to DISABLED") {
            build { initialState = CircuitBreakerState.DISABLED }.initialState shouldBe CircuitBreakerState.DISABLED
        }

        test("automaticTransitionFromOpenToHalfOpen can be enabled") {
            build { automaticTransitionFromOpenToHalfOpen = true }.automaticTransitionFromOpenToHalfOpen shouldBe true
        }
    }
})
