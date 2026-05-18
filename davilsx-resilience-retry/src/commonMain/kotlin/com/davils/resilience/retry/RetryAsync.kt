/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.resilience.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

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
     * If [RetryData.failAfterMaxRetries] is true and all attempts are exhausted by an exception,
     * the last caught exception is rethrown. If all attempts are exhausted by a result-based retry,
     * the behavior is controlled by [RetryData.onResultExhaustion]: either a
     * [MaxRetriesExceededException] is thrown or the last observed value is returned to the caller.
     * If [RetryData.failAfterMaxRetries] is false, the loop continues until [block] returns a value
     * that the predicate accepts.
     *
     * @param T The type of the value produced by [block].
     * @param block The suspending operation to execute.
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
    public suspend fun <T> execute(block: suspend () -> T): T {
        var attempt = 1
        while (true) {
            val result: T
            try {
                result = block()
                if (!shouldRetryOnResult(attempt, result)) return result
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                if (!shouldRetry(attempt, throwable)) throw throwable
            }

            delay(nextDelay(attempt))
            attempt++
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
    val data = retryBuilder.produce()
    return RetryAsync(data)
}
