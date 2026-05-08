package com.davils.resilience.retry

/**
 * Defines the behavior applied when a result-based retry exhausts the configured maximum number of attempts.
 *
 * This setting is only consulted when the executed block did not throw and the configured
 * [com.davils.resilience.retry.predicate.Predicate] still requested another retry through
 * [com.davils.resilience.retry.predicate.Predicate.shouldRetryOnResult] at the moment
 * [RetryData.maxAttempts] was reached, while [RetryData.failAfterMaxRetries] is true.
 *
 * Exception-based exhaustion is unaffected by this setting: the last [Throwable] thrown by the
 * block is always rethrown, since the throwable itself carries the most relevant failure information.
 *
 * @since 1.0.0
 */
public enum class OnResultExhaustion {
    /**
     * Throw a [MaxRetriesExceededException] carrying the last observed result.
     *
     * This is the default behavior because silently returning a value that the predicate has just
     * rejected can mask real failures (for example, a stale HTTP 503 response).
     *
     * @since 1.0.0
     */
    THROW,

    /**
     * Return the last value produced by the executed block, even though the predicate rejected it.
     *
     * This preserves the historical, pre-1.0.0 behavior and is intended for callers that prefer
     * to inspect the value themselves rather than handle a dedicated exception.
     *
     * @since 1.0.0
     */
    RETURN_LAST
}
