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
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

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

    private val metricsMutex = Mutex()
    private val totalCalls = atomic(0L)
    private val successfulCalls = atomic(0L)
    private val exhaustedCalls = atomic(0L)
    private val failedNonRetryableCalls = atomic(0L)
    private val canceledCalls = atomic(0L)
    private val totalAttempts = atomic(0L)
    private val successfulAttempts = atomic(0L)
    private val failedAttempts = atomic(0L)
    private val totalAttemptDurationNanos = atomic(0L)
    private val totalBackoffDurationNanos = atomic(0L)
    private val totalCallDurationNanos = atomic(0L)
    private val callsActive = atomic(0L)
    private val callsWaiting = atomic(0L)

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
     * Returns a snapshot of the current metrics.
     *
     * @since 1.0.0
     */
    public suspend fun getMetrics(): RetryMetrics = metricsMutex.withLock {
        RetryMetrics(
            totalCalls = totalCalls.value,
            successfulCalls = successfulCalls.value,
            exhaustedCalls = exhaustedCalls.value,
            failedNonRetryableCalls = failedNonRetryableCalls.value,
            canceledCalls = canceledCalls.value,
            totalAttempts = totalAttempts.value,
            successfulAttempts = successfulAttempts.value,
            failedAttempts = failedAttempts.value,
            totalAttemptDuration = totalAttemptDurationNanos.value.nanoseconds,
            totalBackoffDuration = totalBackoffDurationNanos.value.nanoseconds,
            totalCallDuration = totalCallDurationNanos.value.nanoseconds,
            callsActive = callsActive.value,
            callsWaiting = callsWaiting.value,
        )
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
        val callStart = TimeSource.Monotonic.markNow()
        totalCalls.incrementAndGet()
        callsActive.incrementAndGet()

        try {
            var attempt = 1

            while (true) {
                if (isDisposed()) {
                    canceledCalls.incrementAndGet()
                    throw CancellationException("Retry instance is disposed")
                }

                totalAttempts.incrementAndGet()
                eventBus.push(RetryEvent.RetryAttemptStarted(attempt))

                val attemptStart = TimeSource.Monotonic.markNow()
                val result: T
                try {
                    result = block()
                    successfulAttempts.incrementAndGet()
                    totalAttemptDurationNanos.addAndGet(attemptStart.elapsedNow().inWholeNanoseconds)

                    if (!shouldRetryOnResult(attempt, result)) {
                        eventBus.push(RetryEvent.RetrySucceeded(attempt))
                        successfulCalls.incrementAndGet()
                        return result
                    }
                } catch (exhausted: MaxRetriesExceededException) {
                    totalAttemptDurationNanos.addAndGet(attemptStart.elapsedNow().inWholeNanoseconds)
                    exhaustedCalls.incrementAndGet()
                    throw exhausted
                } catch (cancellation: CancellationException) {
                    totalAttemptDurationNanos.addAndGet(attemptStart.elapsedNow().inWholeNanoseconds)
                    canceledCalls.incrementAndGet()
                    eventBus.push(RetryEvent.RetryCancelled(attempt, cancellation))
                    throw cancellation
                } catch (throwable: Throwable) {
                    totalAttemptDurationNanos.addAndGet(attemptStart.elapsedNow().inWholeNanoseconds)
                    failedAttempts.incrementAndGet()
                    eventBus.push(RetryEvent.RetryAttemptFailed(attempt, throwable))
                    if (!shouldRetryOnThrowable(attempt, throwable)) {
                        if (attempt >= data.maxAttempts && data.failAfterMaxRetries) {
                            exhaustedCalls.incrementAndGet()
                        } else {
                            failedNonRetryableCalls.incrementAndGet()
                        }
                        eventBus.push(RetryEvent.RetryFailed(attempt, throwable))
                        throw throwable
                    }
                }

                val delayDuration = nextDelay(attempt)
                eventBus.push(RetryEvent.RetryAttemptBackoff(attempt, delayDuration))
                callsWaiting.incrementAndGet()
                try {
                    totalBackoffDurationNanos.addAndGet(delayDuration.inWholeNanoseconds)
                    delay(delayDuration)
                } finally {
                    callsWaiting.decrementAndGet()
                }
                attempt++
            }
        } finally {
            totalCallDurationNanos.addAndGet(callStart.elapsedNow().inWholeNanoseconds)
            callsActive.decrementAndGet()
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
