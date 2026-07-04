package com.davils.resilience.circuitbreaker

import com.davils.kore.pattern.creational.dsl.verification.DslVerificationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class CircuitBreakerDataTest : FunSpec({
    fun build(block: CircuitBreakerBuilder.() -> Unit): CircuitBreakerData {
        val b = CircuitBreakerBuilder().apply {
            // use small valid defaults for test cases that need to override minimumNumberOfCalls
            slidingWindowSize = 10
            minimumNumberOfCalls = 5
        }
        b.apply(block)
        return b.produce()
    }

    context("validation") {
        test("failureRateThreshold below 0 is rejected") {
            shouldThrow<DslVerificationException> {
                build { failureRateThreshold = -1f }
            }
        }

        test("failureRateThreshold above 100 is rejected") {
            shouldThrow<DslVerificationException> {
                build { failureRateThreshold = 101f }
            }
        }

        test("slowCallRateThreshold above 100 is rejected") {
            shouldThrow<DslVerificationException> {
                build { slowCallRateThreshold = 101f }
            }
        }

        test("negative slowCallDurationThreshold is rejected") {
            shouldThrow<DslVerificationException> {
                build { slowCallDurationThreshold = (-1).seconds }
            }
        }

        test("permittedCallsInHalfOpenState of 0 is rejected") {
            shouldThrow<DslVerificationException> {
                build { permittedCallsInHalfOpenState = 0 }
            }
        }

        test("minimumNumberOfCalls of 0 is rejected") {
            shouldThrow<DslVerificationException> {
                build { minimumNumberOfCalls = 0 }
            }
        }

        test("slidingWindowSize of 0 is rejected") {
            shouldThrow<DslVerificationException> {
                build { slidingWindowSize = 0 }
            }
        }

        test("minimumNumberOfCalls > slidingWindowSize for COUNT_BASED is rejected") {
            shouldThrow<DslVerificationException> {
                build { slidingWindowSize = 5; minimumNumberOfCalls = 10 }
            }
        }

        test("minimumNumberOfCalls > slidingWindowSize is allowed for TIME_BASED") {
            val data = build {
                slidingWindowType = SlidingWindowType.TIME_BASED
                slidingWindowSize = 5
                minimumNumberOfCalls = 10
            }
            data.minimumNumberOfCalls shouldBe 10
        }

        test("negative maxWaitDurationInHalfOpenState is rejected") {
            shouldThrow<DslVerificationException> {
                build { maxWaitDurationInHalfOpenState = (-1).seconds }
            }
        }

        test("transitionStateAfterWaitDuration must be OPEN or CLOSED") {
            shouldThrow<DslVerificationException> {
                build { transitionStateAfterWaitDuration = CircuitBreakerState.DISABLED }
            }
        }

        test("valid configuration is accepted") {
            val data = build { }
            data.failureRateThreshold shouldBe 50f
        }
    }
})
