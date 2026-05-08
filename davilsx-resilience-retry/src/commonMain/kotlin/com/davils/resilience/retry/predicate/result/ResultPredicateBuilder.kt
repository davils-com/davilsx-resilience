package com.davils.resilience.retry.predicate.result

import com.davils.kore.annotation.KoreDsl

/**
 * A builder class for creating instances of [ResultPredicateData].
 *
 * This builder provides a DSL-friendly way to configure a result-based retry
 * predicate. The configured condition decides whether a successfully returned
 * value should trigger another retry attempt.
 *
 * Typical usage includes retrying on unsuccessful HTTP responses, sentinel
 * "not ready" values returned from polling endpoints, or any other domain-level
 * outcome that should be treated like a transient failure.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ResultPredicateBuilder<T> internal constructor() {
    private var condition: (T) -> Boolean = { false }

    /**
     * Indicates whether `null` results should be forwarded to the configured condition.
     *
     * When false (the default), a `null` value short-circuits to "do not retry"
     * without invoking the condition. When true, the condition receives `null`
     * and must perform its own null handling.
     *
     * @since 1.0.0
     */
    public var retryOnNull: Boolean = false

    /**
     * Configures the predicate to retry whenever the supplied [condition] returns true for the produced value.
     *
     * Replaces any previously configured condition. The supplied lambda must be
     * pure: it must not mutate external state, must not throw, and should return
     * quickly because it can be invoked on every executed attempt.
     *
     * @param condition A lambda that receives the value produced by the executed block and returns true to trigger a retry.
     * @since 1.0.0
     */
    public fun retryIf(condition: (T) -> Boolean) {
        this.condition = condition
    }

    internal fun build(): ResultPredicateData<T> = ResultPredicateData(condition, retryOnNull)
}
