package com.davils.resilience.circuitbreaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CircuitBreakerRegistryTest : FunSpec({
    fun smallBuilder(): CircuitBreakerBuilder.() -> Unit = {
        slidingWindowSize = 10
        minimumNumberOfCalls = 5
    }

    fun registry() = circuitBreakerRegistry { }

    context("create and retrieve") {
        test("creates a circuit breaker by name") {
            val reg = registry()
            val cb = reg.create("api", builder = smallBuilder())
            cb.shouldNotBeNull()
        }

        test("get returns the created instance") {
            val reg = registry()
            val created = reg.create("api", builder = smallBuilder())
            reg["api"] shouldBe created
        }

        test("lookupOrNull returns null for unknown name") {
            val reg = registry()
            reg.lookupOrNull("unknown").shouldBeNull()
        }

        test("getOrCreate returns the same instance for the same name") {
            val reg = registry()
            reg.create("api", builder = smallBuilder())
            val first = reg["api"]
            val second = reg.getOrCreate("api", builder = smallBuilder())
            first shouldBe second
        }

        test("replace substitutes the existing entry with a new component") {
            val reg = registry()
            val original = reg.create("svc", builder = smallBuilder())
            val replacement = circuitBreaker(smallBuilder())
            reg.replace("svc", replacement)
            val fetched = reg["svc"]
            fetched shouldBe replacement
            (fetched !== original) shouldBe true
        }

        test("clear removes all entries") {
            val reg = registry()
            reg.create("a", builder = smallBuilder())
            reg.create("b", builder = smallBuilder())
            reg.clear()
            reg.lookupOrNull("a").shouldBeNull()
            reg.lookupOrNull("b").shouldBeNull()
        }
    }
})
