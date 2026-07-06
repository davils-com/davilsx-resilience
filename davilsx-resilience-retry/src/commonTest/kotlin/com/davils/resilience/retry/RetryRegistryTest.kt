package com.davils.resilience.retry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class RetryRegistryTest : FunSpec({
    fun registry() = retryRegistry { }

    context("create and retrieve") {
        test("creates a retry policy by name") {
            val reg = registry()
            reg.create("api") { maxAttempts = 3 }.shouldNotBeNull()
        }

        test("get returns the created instance") {
            val reg = registry()
            val created = reg.create("api") { maxAttempts = 3 }
            reg["api"] shouldBe created
        }

        test("lookupOrNull returns null for unknown name") {
            val reg = registry()
            reg.lookupOrNull("unknown").shouldBeNull()
        }

        test("getOrCreate returns the same instance for the same name") {
            val reg = registry()
            reg.create("api") { maxAttempts = 3 }
            val first = reg["api"]
            val second = reg.getOrCreate("api") { maxAttempts = 5 }
            first shouldBe second
        }

        test("replace substitutes the existing entry with a new component") {
            val reg = registry()
            val original = reg.create("svc") { maxAttempts = 3 }
            val replacement = retry { maxAttempts = 5 }
            reg.replace("svc", replacement)
            val fetched = reg["svc"]
            fetched shouldBe replacement
            (fetched !== original) shouldBe true
        }

        test("clear removes all entries") {
            val reg = registry()
            reg.create("a") { maxAttempts = 3 }
            reg.create("b") { maxAttempts = 3 }
            reg.clear()
            reg.lookupOrNull("a").shouldBeNull()
            reg.lookupOrNull("b").shouldBeNull()
        }
    }
})
