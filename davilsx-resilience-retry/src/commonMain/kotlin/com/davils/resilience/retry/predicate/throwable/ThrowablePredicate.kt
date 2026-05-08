package com.davils.resilience.retry.predicate.throwable

import com.davils.resilience.retry.predicate.Predicate

/**
 * A predicate that retries based on a specific set of [Throwable] classes.
 *
 * This implementation checks if the occurred exception is an instance of any
 * of the configured throwable types.
 *
 * @since 1.0.0
 */
public class ThrowablePredicate internal constructor(private val data: ThrowablePredicateData) : Predicate {
    /**
     * Determines whether to retry based on whether the throwable matches any configured type.
     *
     * @param throwable The exception to evaluate.
     * @return true if the throwable matches any of the configured types, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetry(throwable: Throwable?): Boolean = when {
        throwable == null -> false
        data.ignoreThrowables.any { it.isInstance(throwable) } -> false
        data.retryOnAll -> true
        else -> data.throwables.any { it.isInstance(throwable) }
    }
}

/**
 * Creates a [ThrowablePredicate] using a DSL builder.
 *
 * This function allows for easy configuration of which exceptions should trigger a retry.
 *
 * @param builder A lambda to configure the [ThrowablePredicateBuilder].
 * @return A [ThrowablePredicate] configured with the specified throwable types.
 * @since 1.0.0
 */
public fun throwablePredicate(builder: ThrowablePredicateBuilder.() -> Unit): ThrowablePredicate {
    val throwableBuilder = ThrowablePredicateBuilder()
    throwableBuilder.builder()
    val data = throwableBuilder.build()
    return ThrowablePredicate(data)
}
