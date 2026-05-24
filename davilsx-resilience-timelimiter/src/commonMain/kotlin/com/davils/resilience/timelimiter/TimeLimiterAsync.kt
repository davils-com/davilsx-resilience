package com.davils.resilience.timelimiter

import com.davils.resilience.common.DisposableAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


public class TimeLimiterAsync(private val data: TimeLimiterData) : DisposableAsync {
    private val detachedScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    public suspend fun <T>execute(block: suspend () -> T): T? {
        if (data.timeout == Duration.ZERO) return block()

        return when (data.strategy) {
            TimeoutStrategy.HARD -> executeHard(block)
            TimeoutStrategy.SOFT -> executeSoft(block)
        }
    }

    private suspend fun <T>executeHard(block: suspend () -> T): T? {
        try {
            return withTimeout(data.timeout.inWholeMilliseconds.milliseconds) {
                block()
            }
        } catch (_: TimeoutCancellationException) {
            val ex = TimeoutExceededException(data.timeout.inWholeMilliseconds)
            return handleFallbackOrThrow(ex)
        }
    }

    private suspend fun <T>executeSoft(block: suspend () -> T): T? {
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

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T>handleFallbackOrThrow(exception: Throwable): T? {
        val fallback = data.fallback as? (suspend (Throwable) -> T?)
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

    override suspend fun dispose() {
        TODO("Not yet implemented")
    }
}

public fun timeLimiter(builder: TimeLimiterBuilder.() -> Unit): TimeLimiterAsync =
    TimeLimiterAsync(TimeLimiterBuilder().apply(builder).build())
