package com.davils.resilience.retry.predicate.result

/**
 * Data class containing the configuration for a result-based retry predicate.
 *
 * This class is typically instantiated via [ResultPredicateBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ResultPredicateData<T> internal constructor(
    /**
     * The user-supplied condition that decides whether a successful result value should trigger a retry.
     *
     * The condition is invoked for every value returned by the executed block.
     * It must be side-effect free and must not throw, since exceptions raised
     * while evaluating the predicate would otherwise be propagated to the caller.
     *
     * @since 1.0.0
     */
    val condition: (T) -> Boolean,

    /**
     * Indicates whether `null` results are forwarded to [condition].
     *
     * When false (the default), a `null` result short-circuits to `false` without
     * invoking [condition], which keeps user code free from null checks.
     * When true, a `null` value is passed to [condition] as-is, which requires
     * the caller to handle nullability explicitly.
     *
     * @since 1.0.0
     */
    val retryOnNull: Boolean
)
