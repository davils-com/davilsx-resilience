package com.davils.resilience.retry.predicate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AlwaysRetryOnThrowablePredicateTest : FunSpec({
    context("Functions") {
        context("shouldRetry") {

            test("returns true for any non-null throwable") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetry(RuntimeException()) shouldBe true
            }

            test("returns true for any throwable subtype") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetry(IllegalArgumentException()) shouldBe true
                predicate.shouldRetry(NullPointerException()) shouldBe true
                predicate.shouldRetry(Error()) shouldBe true
            }

            test("returns false for null throwable") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetry(null) shouldBe false
            }
        }
    }

    context("alwaysRetryOnThrowablePredicate DSL") {
        test("creates an AlwaysRetryOnThrowablePredicate instance") {
            val predicate = alwaysRetryOnThrowablePredicate()
            predicate.shouldBeInstanceOf<AlwaysRetryOnThrowablePredicate>()
        }

        test("created instance returns true for non-null throwable") {
            val predicate = alwaysRetryOnThrowablePredicate()
            predicate.shouldRetry(RuntimeException()) shouldBe true
        }

        test("created instance returns false for null throwable") {
            val predicate = alwaysRetryOnThrowablePredicate()
            predicate.shouldRetry(null) shouldBe false
        }
    }
})