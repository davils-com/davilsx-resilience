package com.davils.resilience.retry.predicate

/**
 * A predicate that always returns true if a non-null [Throwable] is provided.
 *
 * This implementation is useful when any exception should trigger a retry attempt,
 * regardless of its type or properties.
 *
 * @since 1.0.0
 */
public class AlwaysRetryOnThrowablePredicate internal constructor() : Predicate {
    /**
     * Returns true if the provided throwable is not null.
     *
     * @param throwable The exception to evaluate.
     * @return true if [throwable] is not null, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetry(throwable: Throwable?): Boolean = throwable != null
}

/**
 * Creates an instance of [AlwaysRetryOnThrowablePredicate].
 *
 * @return A [Predicate] that retries on any [Throwable].
 * @since 1.0.0
 */
public fun alwaysRetryOnThrowablePredicate(): AlwaysRetryOnThrowablePredicate = AlwaysRetryOnThrowablePredicate()
