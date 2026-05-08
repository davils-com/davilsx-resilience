package com.davils.resilience.retry.predicate

/**
 * Interface that defines a condition for whether an operation should be retried.
 *
 * Implementations of this interface evaluate a [Throwable] to determine if the
 * failure is transient or otherwise eligible for a retry attempt.
 *
 * @since 1.0.0
 */
public interface Predicate {
    /**
     * Determines whether a retry should be attempted based on the given error.
     *
     * @param throwable The exception that occurred during the operation, or null if no exception was caught.
     * @return true if the operation should be retried, false otherwise.
     * @since 1.0.0
     */
    public fun shouldRetry(throwable: Throwable?): Boolean
}
