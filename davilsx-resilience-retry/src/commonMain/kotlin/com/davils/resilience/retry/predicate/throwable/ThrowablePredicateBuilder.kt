package com.davils.resilience.retry.predicate.throwable

import com.davils.kore.annotation.KoreDsl
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
public class ThrowablePredicateBuilder internal constructor() {
    /**
     * The list of [Throwable] classes to be used for matching.
     *
     * @since 1.0.0
     */
    public var throwables: MutableList<KClass<out Throwable>> = mutableListOf()

    /**
     * The list of [Throwable] classes to be ignored.
     *
     * Exceptions in this list will not trigger a retry, even if they match
     * other retry criteria or if [retryOnAll] is enabled.
     *
     * @since 1.0.0
     */
    public var ignoreThrowables: MutableList<KClass<out Throwable>> = mutableListOf()

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
     * Adds a single [Throwable] class to the retry list.
     *
     * @param throwable The exception class to add.
     * @since 1.0.0
     */
    public fun throwable(throwable: KClass<out Throwable>) {
        this.throwables.add(throwable)
    }

    /**
     * Adds multiple [Throwable] classes to the retry list.
     *
     * @param throwable Vararg of exception classes to add.
     * @since 1.0.0
     */
    public fun throwables(vararg throwable: KClass<out Throwable>) {
        this.throwables.addAll(throwable)
    }

    /**
     * Adds an iterable of [Throwable] classes to the retry list.
     *
     * @param throwables The iterable of exception classes to add.
     * @since 1.0.0
     */
    public fun throwables(throwables: Iterable<KClass<out Throwable>>) {
        this.throwables.addAll(throwables)
    }

    /**
     * Adds a single [Throwable] class to the ignore list.
     *
     * @param throwable The exception class to ignore.
     * @since 1.0.0
     */
    public fun ignore(throwable: KClass<out Throwable>) {
        this.ignoreThrowables.add(throwable)
    }

    /**
     * Adds multiple [Throwable] classes to the ignore list.
     *
     * @param throwable Vararg of exception classes to ignore.
     * @since 1.0.0
     */
    public fun ignore(vararg throwable: KClass<out Throwable>) {
        this.ignoreThrowables.addAll(throwable)
    }

    /**
     * Adds an iterable of [Throwable] classes to the ignore list.
     *
     * @param throwables The iterable of exception classes to ignore.
     * @since 1.0.0
     */
    public fun ignore(throwables: Iterable<KClass<out Throwable>>) {
        this.ignoreThrowables.addAll(throwables)
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

    internal fun build() = ThrowablePredicateData(throwables, ignoreThrowables, retryOnAll)
}
