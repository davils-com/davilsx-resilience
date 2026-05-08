package com.davils.resilience.retry.predicate.throwable

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ThrowablePredicateTest : FunSpec({
    context("Functions") {
        context("shouldRetry") {

            test("returns false for null throwable") {
                val predicate = throwablePredicate { throwable(RuntimeException::class) }
                predicate.shouldRetry(null) shouldBe false
            }

            test("returns true when throwable matches registered type") {
                val predicate = throwablePredicate { throwable(RuntimeException::class) }
                predicate.shouldRetry(RuntimeException("error")) shouldBe true
            }

            test("returns true when throwable is a subclass of registered type") {
                val predicate = throwablePredicate { throwable(Exception::class) }
                predicate.shouldRetry(IllegalArgumentException("sub")) shouldBe true
            }

            test("returns false when throwable does not match any registered type") {
                val predicate = throwablePredicate { throwable(IllegalArgumentException::class) }
                predicate.shouldRetry(RuntimeException("other")) shouldBe false
            }

            test("returns true when throwable matches one of multiple registered types") {
                val predicate = throwablePredicate {
                    throwables(IllegalArgumentException::class, NullPointerException::class)
                }
                predicate.shouldRetry(NullPointerException()) shouldBe true
            }

            test("returns false when no throwables are registered") {
                val predicate = throwablePredicate {}
                predicate.shouldRetry(RuntimeException()) shouldBe false
            }
        }
    }

    context("throwablePredicate DSL") {
        test("creates a ThrowablePredicate instance") {
            val predicate = throwablePredicate { throwable(RuntimeException::class) }
            predicate shouldNotBe null
        }

        test("creates predicate with empty throwables list when no throwables added") {
            val predicate = throwablePredicate {}
            predicate.shouldRetry(RuntimeException()) shouldBe false
        }
    }
})
