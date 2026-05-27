package com.davils.resilience.retry.predicate.result

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ResultPredicateTest : FunSpec({
    context("shouldRetry(throwable)") {
        test("always returns false regardless of input") {
            val predicate = resultPredicate<Int> { retryIf { it < 0 } }
            predicate.shouldRetryOnThrowable(null) shouldBe false
            predicate.shouldRetryOnThrowable(RuntimeException()) shouldBe false
        }
    }

    context("shouldRetryOnResult") {
        test("returns true when condition matches the value") {
            val predicate = resultPredicate<Int> { retryIf { it < 0 } }
            predicate.shouldRetryOnResult(-1) shouldBe true
        }

        test("returns false when condition does not match the value") {
            val predicate = resultPredicate<Int> { retryIf { it < 0 } }
            predicate.shouldRetryOnResult(5) shouldBe false
        }

        test("returns false for null when retryOnNull is disabled") {
            val predicate = resultPredicate<String> { retryIf { it.isEmpty() } }
            predicate.shouldRetryOnResult(null) shouldBe false
        }

        test("forwards null to condition when retryOnNull is enabled") {
            val predicate = resultPredicate<String?> {
                retryOnNull = true
                retryIf { it == null }
            }
            predicate.shouldRetryOnResult(null) shouldBe true
        }

        test("works with custom data classes") {
            data class HttpResponse(val status: Int)
            val predicate = resultPredicate<HttpResponse> {
                retryIf { it.status in 500..599 }
            }
            predicate.shouldRetryOnResult(HttpResponse(503)) shouldBe true
            predicate.shouldRetryOnResult(HttpResponse(200)) shouldBe false
        }
    }

    context("resultPredicate DSL") {
        test("default condition does not trigger retries") {
            val predicate = resultPredicate<Int> {}
            predicate.shouldRetryOnResult(1) shouldBe false
            predicate.shouldRetryOnResult(-1) shouldBe false
        }
    }
})
