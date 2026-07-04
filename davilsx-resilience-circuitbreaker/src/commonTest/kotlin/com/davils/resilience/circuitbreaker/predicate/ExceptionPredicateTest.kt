package com.davils.resilience.circuitbreaker.predicate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExceptionPredicateTest : FunSpec({
    context("default predicates") {
        test("recordAllExceptions matches every throwable") {
            recordAllExceptions.test(RuntimeException()) shouldBe true
            recordAllExceptions.test(IllegalArgumentException()) shouldBe true
        }

        test("ignoreNoExceptions never matches") {
            ignoreNoExceptions.test(RuntimeException()) shouldBe false
        }

        test("recordNoResults never matches") {
            recordNoResults.test("anything") shouldBe false
            recordNoResults.test(null) shouldBe false
        }
    }

    context("ExceptionPredicateBuilder") {
        test("empty builder matches all when used as record — falls back to recordAll") {
            val pred = ExceptionPredicateBuilder().build()
            pred.test(RuntimeException()) shouldBe true
        }

        test("single type matches exact class") {
            val pred = ExceptionPredicateBuilder().apply { on<IllegalArgumentException>() }.build()
            pred.test(IllegalArgumentException()) shouldBe true
            pred.test(RuntimeException()) shouldBe false
        }

        test("subclass matches registered superclass") {
            val pred = ExceptionPredicateBuilder().apply { on<RuntimeException>() }.build()
            pred.test(IllegalArgumentException()) shouldBe true
        }

        test("cause chain is not checked by default") {
            val cause = IllegalArgumentException("root")
            val wrapper = RuntimeException("wrapper", cause)
            val pred = ExceptionPredicateBuilder().apply { on<IllegalArgumentException>() }.build()
            // Without includeCauseChain, only the top-level exception is checked
            pred.test(wrapper) shouldBe false
        }

        test("cause chain is checked when includeCauseChain is enabled") {
            val cause = IllegalArgumentException("root")
            val wrapper = RuntimeException("wrapper", cause)
            val pred = ExceptionPredicateBuilder().apply {
                on<IllegalArgumentException>()
                includeCauseChain()
            }.build()
            pred.test(wrapper) shouldBe true
        }
    }
})
