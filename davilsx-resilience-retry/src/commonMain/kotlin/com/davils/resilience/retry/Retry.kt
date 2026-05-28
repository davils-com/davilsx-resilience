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

import com.davils.resilience.common.ResilienceComponent
import com.davils.resilience.retry.event.RetryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * A resilience component that automatically retries failed operations based on a configurable policy.
 *
 * [Retry] encapsulates logic for handling transient failures by re-executing a block of code
 * according to a [com.davils.resilience.retry.predicate.Predicate] and a
 * [com.davils.resilience.retry.strategy.BackoffStrategy]. It provides a comprehensive event
 * system for monitoring the retry lifecycle.
 *
 * Instances of this class are thread-safe and can be reused for multiple executions.
 *
 * @since 1.0.0
 */
public class Retry internal constructor(
    override val data: RetryData
) : ResilienceComponent<RetryData, RetryEvent>() {
    override val disposeEvent: RetryEvent
        get() = RetryEvent.RetryDisposed

    private fun shouldRetryOnThrowable(attempt: Int, throwable: Throwable): Boolean {
        if (!data.predicate.shouldRetryOnThrowable(throwable)) return false
        if (attempt >= data.maxAttempts && data.failAfterMaxRetries) return false
        return true
    }

    private fun shouldRetryOnResult(attempt: Int, result: Any?): Boolean {
        if (!data.predicate.shouldRetryOnResult(result)) return false
        if (attempt >= data.maxAttempts && data.failAfterMaxRetries) {
            return when (data.onResultExhaustion) {
                OnResultExhaustion.THROW -> throw MaxRetriesExceededException(attempt, result)
                OnResultExhaustion.RETURN_LAST -> false
            }
        }
        return true
    }

    private fun nextDelay(nextAttempt: Int): Duration {
        val delay = data.backoffStrategy.calculateDelay(nextAttempt)
        return if (delay < Duration.ZERO) Duration.ZERO else delay
    }

    /**
     * Executes the given [block] and retries it if it fails according to the configured policy.
     *
     * This method will suspend until the operation succeeds, fails permanently, or is cancelled.
     * Events are emitted to the [eventBus] throughout the execution.
     *
     * @param T The return type of the block.
     * @param block The suspendable operation to execute.
     * @return The result of the successful operation.
     * @throws Exception The last exception encountered if all retry attempts fail.
     * @throws MaxRetriesExceededException If result-based retry exhaustion is configured to throw.
     * @throws CancellationException If the retry instance is disposed or the calling coroutine is cancelled.
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        var attempt = 1

        while (true) {
            if (isDisposed()) {
                throw CancellationException("Retry instance is disposed")
            }

            eventBus.push(RetryEvent.RetryAttemptStarted(attempt))

            val result: T
            try {
                result = block()
                if (!shouldRetryOnResult(attempt, result)) {
                    eventBus.push(RetryEvent.RetrySucceeded(attempt))
                    return result
                }
            } catch (cancellation: CancellationException) {
                eventBus.push(RetryEvent.RetryCancelled(attempt, cancellation))
                throw cancellation
            } catch (throwable: Throwable) {
                eventBus.push(RetryEvent.RetryAttemptFailed(attempt, throwable))
                if (!shouldRetryOnThrowable(attempt, throwable)) {
                    eventBus.push(RetryEvent.RetryFailed(attempt, throwable))
                    throw throwable
                }
            }

            val delayDuration = nextDelay(attempt)
            eventBus.push(RetryEvent.RetryAttemptBackoff(attempt, delayDuration))
            delay(delayDuration)
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
