package com.davils.resilience.retry.predicate.composite

import com.davils.resilience.retry.predicate.result.resultPredicate
import com.davils.resilience.retry.predicate.throwable.throwablePredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CompositePredicateTest : FunSpec({
    val illegalState = throwablePredicate { throwable(IllegalStateException::class) }
    val retryBadResult = resultPredicate<String> { retryIf { it == "bad" } }

    context("anyOf") {
        test("returns true when any delegate opts in on throwable") {
            anyOf(illegalState, retryBadResult).shouldRetryOnThrowable(IllegalStateException()) shouldBe true
        }

        test("returns true when any delegate opts in on result") {
            anyOf(illegalState, retryBadResult).shouldRetryOnResult("bad") shouldBe true
        }

        test("returns false when no delegate opts in") {
            val composite = anyOf(illegalState, retryBadResult)
            composite.shouldRetryOnThrowable(RuntimeException()) shouldBe false
            composite.shouldRetryOnResult("ok") shouldBe false
        }

        test("empty anyOf never opts in") {
            anyOf().shouldRetryOnThrowable(RuntimeException()) shouldBe false
            anyOf().shouldRetryOnResult("value") shouldBe false
        }
    }

    context("allOf") {
        test("requires every delegate to opt in on throwable") {
            allOf(illegalState, retryBadResult).shouldRetryOnThrowable(IllegalStateException()) shouldBe false
        }

        test("returns false when one delegate rejects on result") {
            allOf(illegalState, retryBadResult).shouldRetryOnResult("bad") shouldBe false
        }

        test("empty allOf always opts in") {
            allOf().shouldRetryOnThrowable(null) shouldBe true
            allOf().shouldRetryOnResult(null) shouldBe true
        }
    }

    context("infix operators") {
        test("or combines with ANY semantics") {
            (illegalState or retryBadResult).shouldRetryOnResult("bad") shouldBe true
        }

        test("and combines with ALL semantics") {
            (illegalState and retryBadResult).shouldRetryOnResult("bad") shouldBe false
        }
    }
})
