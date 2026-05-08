package com.davils.resilience.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Coroutine-based, thread-safe retry executor.
 *
 * Each invocation of [execute] is fully isolated: all attempt counters and intermediate state
 * live in local variables on the calling coroutine. The provider instance itself is immutable,
 * which makes it safe to share a single [RetryAsync] across multiple coroutines and threads.
 *
 * The shared retry decision logic lives in [RetryProvider]. This class only adds the suspending
 * orchestration of the retry loop, including the cooperative wait between attempts and proper
 * propagation of coroutine cancellation.
 *
 * @since 1.0.0
 */
public class RetryAsync(override val data: RetryData) : RetryProvider {
    /**
     * Executes the given suspending [block] applying the configured retry policy.
     *
     * The block is invoked at least once. If it throws and the configured predicate, attempt
     * limit and [RetryData.failAfterMaxRetries] flag indicate that another attempt should be
     * performed, the coroutine suspends for the duration returned by the configured backoff
     * strategy before the next attempt is executed.
     *
     * Cooperative cancellation is fully respected: a [kotlinx.coroutines.CancellationException]
     * is never retried and is always rethrown immediately. Before each retry, the coroutine
     * context is also checked for cancellation.
     *
     * If [RetryData.failAfterMaxRetries] is true and all attempts are exhausted, the last
     * caught exception is rethrown. If it is false, the loop continues until [block] returns
     * successfully.
     *
     * @param T The type of the value produced by [block].
     * @param block The suspending operation to execute.
     * @return The value produced by the first successful invocation of [block].
     * @throws Throwable The last exception thrown by [block] when retries are exhausted and
     * [RetryData.failAfterMaxRetries] is true, or any non-retryable exception thrown by [block].
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        var attempt = 1
        while (true) {
            try {
                return block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (!shouldRetry(attempt, throwable)) throw throwable
                val waitDuration = nextDelay(attempt)
                delay(waitDuration.coerceAtLeast(Duration.ZERO))
                attempt++
            }
        }
    }
}

/**
 * Creates a new [RetryAsync] instance configured by the given [builder].
 *
 * @param builder Configuration block applied to a [RetryBuilder].
 * @return A fully configured, immutable [RetryAsync] instance.
 * @since 1.0.0
 */
public fun retryAsync(builder: RetryBuilder.() -> Unit): RetryAsync {
    val retryBuilder = RetryBuilder()
    retryBuilder.builder()
    val data = retryBuilder.build()
    return RetryAsync(data)
}
