/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.resilience.retry.predicate.throwable

import com.davils.kore.annotation.KoreDsl
import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import kotlin.reflect.KClass

/**
 * A builder class for creating instances of [ThrowablePredicateData].
 *
 * This builder provides a DSL-friendly way to configure the list of exception
 * types that should trigger a retry.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ThrowablePredicateBuilder internal constructor() : DslValidator<ThrowablePredicateData>() {
    /**
     * The list of [Throwable] classes to be used for matching.
     *
     * @since 1.0.0
     */
    public var throwables: List<KClass<out Throwable>> = mutableListOf()

    /**
     * The list of [Throwable] classes to be ignored.
     *
     * Exceptions in this list will not trigger a retry, even if they match
     * other retry criteria or if [retryOnAll] is enabled.
     *
     * @since 1.0.0
     */
    public var ignoreThrowables: List<KClass<out Throwable>> = mutableListOf()

    /**
     * Flag indicating whether all exceptions should trigger a retry attempt.
     *
     * Defaults to false. If true, any [Throwable] will result in a retry unless
     * it is explicitly listed in [ignoreThrowables].
     *
     * @since 1.0.0
     */
    public var retryOnAll: Boolean = false

    /**
     * Flag indicating whether the cause chain of an evaluated exception should be inspected.
     *
     * When true, both the retry list and the ignore list are matched against
     * the exception itself and every exception reachable via [Throwable.cause].
     * Defaults to false to preserve historical behavior.
     *
     * @since 1.0.0
     */
    public var includeCauseChain: Boolean = false

    /**
     * Adds a single [Throwable] class to the retry list.
     *
     * @param throwable The exception class to add.
     * @since 1.0.0
     */
    public fun throwable(throwable: KClass<out Throwable>) {
        this.throwables += throwable
    }

    /**
     * Adds multiple [Throwable] classes to the retry list.
     *
     * @param throwable Vararg of exception classes to add.
     * @since 1.0.0
     */
    public fun throwables(vararg throwable: KClass<out Throwable>) {
        this.throwables += throwable
    }

    /**
     * Adds an iterable of [Throwable] classes to the retry list.
     *
     * @param throwables The iterable of exception classes to add.
     * @since 1.0.0
     */
    public fun throwables(throwables: Iterable<KClass<out Throwable>>) {
        this.throwables += throwables
    }

    /**
     * Adds a single [Throwable] class to the ignore list.
     *
     * @param throwable The exception class to ignore.
     * @since 1.0.0
     */
    public fun ignore(throwable: KClass<out Throwable>) {
        this.ignoreThrowables += throwable
    }

    /**
     * Adds multiple [Throwable] classes to the ignore list.
     *
     * @param throwable Vararg of exception classes to ignore.
     * @since 1.0.0
     */
    public fun ignore(vararg throwable: KClass<out Throwable>) {
        this.ignoreThrowables += throwable
    }

    /**
     * Adds an iterable of [Throwable] classes to the ignore list.
     *
     * @param throwables The iterable of exception classes to ignore.
     * @since 1.0.0
     */
    public fun ignore(throwables: Iterable<KClass<out Throwable>>) {
        this.ignoreThrowables += throwables
    }

    /**
     * Configures the predicate to retry on all exceptions.
     *
     * When called, [retryOnAll] is set to true. Specific exceptions can still
     * be excluded by adding them to the ignore list.
     *
     * @since 1.0.0
     */
    public fun retryOnAll() {
        retryOnAll = true
    }

    /**
     * Enables matching against the cause chain of evaluated exceptions.
     *
     * When invoked, [includeCauseChain] is set to true so that the resulting
     * predicate inspects every exception reachable via [Throwable.cause] when
     * deciding whether to retry or to ignore.
     *
     * @since 1.0.0
     */
    public fun causeChain() {
        includeCauseChain = true
    }

    override fun data(): ThrowablePredicateData = ThrowablePredicateData(
        throwables,
        ignoreThrowables,
        retryOnAll,
        includeCauseChain
    )
}
