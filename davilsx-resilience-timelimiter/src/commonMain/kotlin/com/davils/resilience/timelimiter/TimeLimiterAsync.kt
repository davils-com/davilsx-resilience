package com.davils.resilience.timelimiter

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


@Suppress("UNCHECKED_CAST")
public class TimeLimiterAsync(override val data: TimeLimiterData) : TimeLimiterProvider {
    private val detachedScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    public suspend fun <T> execute(block: suspend () -> T): T {
        if (data.timeout == Duration.ZERO) return block()

        return when (data.strategy) {
            TimeoutStrategy.HARD -> executeHard(block)
            TimeoutStrategy.SOFT -> executeSoft(block)
        } as T
    }

    private suspend fun <T> executeHard(block: suspend () -> T): Any? {
        try {
            return withTimeout(data.timeout.inWholeMilliseconds.milliseconds) {
                block()
            }
        } catch (_: TimeoutCancellationException) {
            val ex = TimeoutExceededException(data.timeout.inWholeMilliseconds)
            return handleFallbackOrThrow(ex)
        }
    }

    private suspend fun <T> executeSoft(block: suspend () -> T): Any? {
        val deferred = detachedScope.async {
            block()
        }

        try {
            return withTimeout(data.timeout.inWholeMilliseconds.milliseconds) {
                deferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            if (data.cancelOnTimeout) deferred.cancel()
            val ex = TimeoutExceededException(data.timeout.inWholeMilliseconds)
            return handleFallbackOrThrow(ex)
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
    }

    private suspend fun handleFallbackOrThrow(exception: Throwable): Any? {
        val fallback = data.fallback
        return if (fallback != null) {
            try {
                fallback(exception)
            } catch (throwable: Throwable) {
                throw throwable
            }
        } else {
            throw exception
        }
    }
}

public fun timeLimiter(builder: TimeLimiterBuilder.() -> Unit): TimeLimiterAsync = TimeLimiterAsync(TimeLimiterBuilder().apply(builder).build())
