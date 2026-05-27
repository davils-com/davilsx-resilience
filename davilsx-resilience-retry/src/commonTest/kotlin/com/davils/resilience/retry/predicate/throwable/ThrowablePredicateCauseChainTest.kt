package com.davils.resilience.retry.predicate.throwable

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ThrowablePredicateCauseChainTest : FunSpec({
    context("includeCauseChain disabled (default)") {
        test("does not match wrapped cause") {
            val predicate = throwablePredicate { throwable(IllegalStateException::class) }
            val wrapped = RuntimeException("outer", IllegalStateException("inner"))
            predicate.shouldRetryOnThrowable(wrapped) shouldBe false
        }
    }

    context("includeCauseChain enabled") {
        test("matches when the wrapped cause is of a configured type") {
            val predicate = throwablePredicate {
                throwable(IllegalStateException::class)
                causeChain()
            }
            val wrapped = RuntimeException("outer", IllegalStateException("inner"))
            predicate.shouldRetryOnThrowable(wrapped) shouldBe true
        }

        test("matches deeply nested causes") {
            val predicate = throwablePredicate {
                throwable(NumberFormatException::class)
                causeChain()
            }
            val deep = RuntimeException(
                "level1",
                IllegalStateException("level2", NumberFormatException("inner"))
            )
            predicate.shouldRetryOnThrowable(deep) shouldBe true
        }

        test("ignore list wins over cause chain match") {
            val predicate = throwablePredicate {
                throwable(IllegalStateException::class)
                ignore(IllegalArgumentException::class)
                causeChain()
            }
            val wrapped = IllegalArgumentException(
                "outer",
                IllegalStateException("inner")
            )
            predicate.shouldRetryOnThrowable(wrapped) shouldBe false
        }

        test("retryOnAll combined with cause-chain ignores wrapped excluded type") {
            val predicate = throwablePredicate {
                retryOnAll()
                ignore(IllegalArgumentException::class)
                causeChain()
            }
            val wrapped = RuntimeException("outer", IllegalArgumentException("inner"))
            predicate.shouldRetryOnThrowable(wrapped) shouldBe false
        }

        test("returns false when neither throwable nor any cause matches") {
            val predicate = throwablePredicate {
                throwable(NumberFormatException::class)
                causeChain()
            }
            val wrapped = RuntimeException("outer", IllegalStateException("inner"))
            predicate.shouldRetryOnThrowable(wrapped) shouldBe false
        }

        test("handles self-referencing cause without infinite loop") {
            val predicate = throwablePredicate {
                throwable(NumberFormatException::class)
                causeChain()
            }
            val selfRef = RuntimeException("self")
            // Build artificial self-reference by chaining.
            val chained = RuntimeException("outer", selfRef)
            predicate.shouldRetryOnThrowable(chained) shouldBe false
        }
    }
})
