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

    internal fun build() = ThrowablePredicateData(throwables)
}
