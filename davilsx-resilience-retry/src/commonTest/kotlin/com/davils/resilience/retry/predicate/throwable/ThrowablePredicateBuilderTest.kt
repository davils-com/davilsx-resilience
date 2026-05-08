package com.davils.resilience.retry.predicate.throwable

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll

class ThrowablePredicateBuilderTest : FunSpec({
    context("Functions") {
        context("throwable") {
            test("adds a single throwable class to the list") {
                val builder = ThrowablePredicateBuilder()
                builder.throwable(RuntimeException::class)
                builder.throwables shouldContain RuntimeException::class
            }

            test("adds multiple throwable classes via repeated calls") {
                val builder = ThrowablePredicateBuilder()
                builder.throwable(RuntimeException::class)
                builder.throwable(IllegalArgumentException::class)
                builder.throwables shouldContainAll listOf(RuntimeException::class, IllegalArgumentException::class)
            }
        }

        context("throwables(vararg)") {
            test("adds multiple throwable classes at once") {
                val builder = ThrowablePredicateBuilder()
                builder.throwables(RuntimeException::class, NullPointerException::class)
                builder.throwables shouldContainAll listOf(RuntimeException::class, NullPointerException::class)
            }

            test("adds no throwables when vararg is empty") {
                val builder = ThrowablePredicateBuilder()
                builder.throwables()
                builder.throwables.shouldBeEmpty()
            }
        }

        context("throwables(Iterable)") {
            test("adds throwables from an iterable") {
                val builder = ThrowablePredicateBuilder()
                builder.throwables(listOf(RuntimeException::class, IllegalStateException::class))
                builder.throwables shouldContainAll listOf(RuntimeException::class, IllegalStateException::class)
            }

            test("adds no throwables for empty iterable") {
                val builder = ThrowablePredicateBuilder()
                builder.throwables(emptyList())
                builder.throwables.shouldBeEmpty()
            }
        }

        context("build") {
            test("builds ThrowablePredicateData with configured throwables") {
                val builder = ThrowablePredicateBuilder()
                builder.throwable(RuntimeException::class)
                val data = builder.build()
                data.throwables shouldContain RuntimeException::class
            }

            test("builds ThrowablePredicateData with empty list when no throwables added") {
                val builder = ThrowablePredicateBuilder()
                val data = builder.build()
                data.throwables.shouldBeEmpty()
            }
        }
    }

    context("Variables") {
        test("throwables list is empty by default") {
            val builder = ThrowablePredicateBuilder()
            builder.throwables.shouldBeEmpty()
        }

        test("throwables list is mutable") {
            val builder = ThrowablePredicateBuilder()
            builder.throwables = mutableListOf(RuntimeException::class)
            builder.throwables shouldContain RuntimeException::class
        }
    }
})
