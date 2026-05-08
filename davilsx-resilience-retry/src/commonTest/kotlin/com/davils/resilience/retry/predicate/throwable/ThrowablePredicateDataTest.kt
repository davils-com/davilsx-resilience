package com.davils.resilience.retry.predicate.throwable

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ThrowablePredicateDataTest : FunSpec({
    context("Variables") {
        test("stores the provided throwables list") {
            val list = mutableListOf<kotlin.reflect.KClass<out Throwable>>(RuntimeException::class)
            val data = ThrowablePredicateData(list)
            data.throwables shouldBe list
        }

        test("holds an empty throwables list when constructed with empty list") {
            val data = ThrowablePredicateData(mutableListOf())
            data.throwables.shouldBeEmpty()
        }
    }

    context("Functions") {
        test("data class equality holds for same throwables list") {
            val list = mutableListOf<kotlin.reflect.KClass<out Throwable>>(IllegalArgumentException::class)
            val data1 = ThrowablePredicateData(list)
            val data2 = ThrowablePredicateData(list)
            data1 shouldBe data2
        }

        test("data class copy produces equal object") {
            val data = ThrowablePredicateData(mutableListOf(RuntimeException::class))
            val copy = data.copy()
            copy shouldBe data
        }

        test("data class copy with different list produces different object") {
            val data = ThrowablePredicateData(mutableListOf(RuntimeException::class))
            val copy = data.copy(throwables = mutableListOf(IllegalStateException::class))
            copy shouldNotBe data
        }
    }
})