package com.davils.resilience.retry.predicate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AlwaysRetryOnThrowablePredicateTest : FunSpec({
    context("Functions") {
        context("shouldRetry") {

            test("returns true for any non-null throwable") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetryOnThrowable(RuntimeException()) shouldBe true
            }

            test("returns true for any throwable subtype") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetryOnThrowable(IllegalArgumentException()) shouldBe true
                predicate.shouldRetryOnThrowable(NullPointerException()) shouldBe true
                predicate.shouldRetryOnThrowable(Error()) shouldBe true
            }

            test("returns false for null throwable") {
                val predicate = AlwaysRetryOnThrowablePredicate()
                predicate.shouldRetryOnThrowable(null) shouldBe false
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
            predicate.shouldRetryOnThrowable(RuntimeException()) shouldBe true
        }

        test("created instance returns false for null throwable") {
            val predicate = alwaysRetryOnThrowablePredicate()
            predicate.shouldRetryOnThrowable(null) shouldBe false
        }
    }
})
