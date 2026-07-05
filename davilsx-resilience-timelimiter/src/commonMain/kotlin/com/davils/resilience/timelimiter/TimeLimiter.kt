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

package com.davils.resilience.timelimiter

import com.davils.resilience.common.ResilienceComponent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource

/**
 * A resilience component that limits the execution time of operations.
 *
 * Supports hard and soft timeout strategies. When a timeout occurs, an optional fallback
 * can return a substitute value; otherwise [TimeoutExceededException] is thrown.
 *
 * @since 1.0.0
 */
public class TimeLimiter internal constructor(
    public override val data: TimeLimiterData,
) : ResilienceComponent<TimeLimiterData, TimeLimiterEvent>() {
    override val disposeEvent: TimeLimiterEvent
        get() = TimeLimiterEvent.TimeLimiterDisposed

    private val scopeJob: Job = SupervisorJob()
    private val detachedScope: CoroutineScope = CoroutineScope(scopeJob + Dispatchers.Default)

    private var runtimeTimeout: Duration = data.timeout

    private val successfulCalls = atomic(0L)
    private val timeoutCalls = atomic(0L)
    private val totalExecutionNanos = atomic(0L)

    /**
     * Executes [block] within the configured timeout.
     *
     * @param T Return type of the block.
     * @param block The operation to execute.
     * @return The result of [block], or the fallback value when a timeout occurs.
     * @throws TimeoutExceededException When the timeout is exceeded and no fallback is configured.
     * @throws CancellationException When the component is disposed or the coroutine is cancelled.
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        if (isDisposed()) {
            throw CancellationException("TimeLimiter instance is disposed")
        }

        val timeout = mutex.withLock { runtimeTimeout }
        if (timeout == Duration.ZERO) {
            val start = TimeSource.Monotonic.markNow()
            return try {
                val result = block()
                recordSuccess(start.elapsedNow())
                result
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }

        return when (data.strategy) {
            TimeoutStrategy.HARD -> executeHard(block, timeout)
            TimeoutStrategy.SOFT -> executeSoft(block, timeout)
        }
    }

    /**
     * Returns a snapshot of the current metrics.
     *
     * @since 1.0.0
     */
    public suspend fun getMetrics(): TimeLimiterMetrics = mutex.withLock {
        TimeLimiterMetrics(
            timeout = runtimeTimeout,
            strategy = data.strategy,
            cancelOnTimeout = data.cancelOnTimeout,
            numberOfSuccessfulCalls = successfulCalls.value,
            numberOfTimeoutCalls = timeoutCalls.value,
            totalExecutionTime = totalExecutionNanos.value.nanoseconds,
        )
    }

    /**
     * Updates the timeout duration at runtime.
     *
     * @since 1.0.0
     */
    public suspend fun changeTimeout(timeout: Duration) {
        require(!timeout.isNegative()) { "timeout must be non-negative" }
        mutex.withLock {
            runtimeTimeout = timeout
        }
    }

    override suspend fun dispose() {
        super.dispose()
        detachedScope.cancel()
    }

    private suspend fun <T> executeHard(block: suspend () -> T, timeout: Duration): T {
        val start = TimeSource.Monotonic.markNow()
        return try {
            val result = withTimeout(timeout) { block() }
            recordSuccess(start.elapsedNow())
            result
        } catch (_: TimeoutCancellationException) {
            handleTimeout(timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
    }

    private suspend fun <T> executeSoft(block: suspend () -> T, timeout: Duration): T {
        val start = TimeSource.Monotonic.markNow()
        val deferred = detachedScope.async { block() }

        return try {
            val result = withTimeout(timeout) { deferred.await() }
            recordSuccess(start.elapsedNow())
            result
        } catch (_: TimeoutCancellationException) {
            if (data.cancelOnTimeout) deferred.cancel()
            handleTimeout(timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
    }

    private suspend fun <T> handleTimeout(timeout: Duration): T {
        timeoutCalls.incrementAndGet()
        eventBus.push(TimeLimiterEvent.TimeoutExceeded(timeout.inWholeMilliseconds))
        val exception = TimeoutExceededException(timeout.inWholeMilliseconds)
        return handleFallbackOrThrow(exception)
    }

    private fun recordSuccess(duration: Duration) {
        successfulCalls.incrementAndGet()
        totalExecutionNanos.addAndGet(duration.inWholeNanoseconds)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> handleFallbackOrThrow(exception: Throwable): T {
        val fallback = data.fallback as? (suspend (Throwable) -> T?)
        return if (fallback != null) {
            fallback(exception) as T
        } else {
            throw exception
        }
    }
}

/**
 * Creates a new [TimeLimiter] instance using the provided configuration [builder].
 *
 * @param builder The configuration builder block.
 * @return A new [TimeLimiter] instance.
 * @since 1.0.0
 */
public fun timeLimiter(builder: TimeLimiterBuilder.() -> Unit): TimeLimiter =
    TimeLimiter(TimeLimiterBuilder().apply(builder).produce())
