package com.davils.resilience.retry

/**
 * Synchronous, thread-safe retry executor.
 *
 * Each invocation of [execute] is fully isolated: all attempt counters and intermediate state
 * are stored in local variables on the calling thread's stack. The provider instance itself is
 * immutable, which makes it safe to share a single [Retry] across multiple threads.
 *
 * The shared retry decision logic lives in [RetryProvider]. This class only adds the blocking
 * orchestration of the retry loop, including the blocking wait between attempts.
 *
 * @since 1.0.0
 */
public class Retry(override val data: RetryData) : RetryProvider {
    /**
     * Executes the given [block] applying the configured retry policy.
     *
     * The block is invoked at least once. If it throws and the configured predicate, attempt
     * limit and [RetryData.failAfterMaxRetries] flag indicate that another attempt should be
     * performed, the calling thread is blocked for the duration returned by the configured
     * backoff strategy before the next attempt is executed.
     *
     * If [RetryData.failAfterMaxRetries] is true and all attempts are exhausted by an exception,
     * the last caught exception is rethrown. If all attempts are exhausted by a result-based retry,
     * the behavior is controlled by [RetryData.onResultExhaustion]: either a
     * [MaxRetriesExceededException] is thrown or the last observed value is returned to the caller.
     * If [RetryData.failAfterMaxRetries] is false, the loop continues until [block] returns a value
     * that the predicate accepts.
     *
     * @param T The type of the value produced by [block].
     * @param block The operation to execute.
     * @return The value produced by an invocation of [block] that is accepted by the configured
     * predicate, or the last observed value when [RetryData.onResultExhaustion] is
     * [OnResultExhaustion.RETURN_LAST] and result-based retries are exhausted.
     * @throws Throwable The last exception thrown by [block] when retries are exhausted and
     * [RetryData.failAfterMaxRetries] is true, or any non-retryable exception thrown by [block].
     * @throws MaxRetriesExceededException When result-based retries are exhausted,
     * [RetryData.failAfterMaxRetries] is true and [RetryData.onResultExhaustion] is
     * [OnResultExhaustion.THROW].
     * @since 1.0.0
     */
    public fun <T> execute(block: () -> T): T {
        var attempt = 1
        while (true) {
            val result: T
            try {
                result = block()
                if (!shouldRetryOnResult(attempt, result)) return result
            } catch (throwable: Throwable) {
                if (!shouldRetry(attempt, throwable)) throw throwable
            }

            val waitDuration = nextDelay(attempt)
            if (waitDuration.isPositive()) {
                Thread.sleep(waitDuration.inWholeMilliseconds)
            }
            attempt++
        }
    }
}

/**
 * Creates a new [Retry] instance configured by the given [builder].
 *
 * @param builder Configuration block applied to a [RetryBuilder].
 * @return A fully configured, immutable [Retry] instance.
 * @since 1.0.0
 */
public fun retry(builder: RetryBuilder.() -> Unit): Retry {
    val retryBuilder = RetryBuilder()
    retryBuilder.builder()
    val data = retryBuilder.produce()
    return Retry(data)
}
