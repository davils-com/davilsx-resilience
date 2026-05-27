package com.davils.resilience.timelimiter

import com.davils.resilience.common.ResilienceComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


public class TimeLimiter internal constructor(
    override val data: TimeLimiterData
) : ResilienceComponent<TimeLimiterData, TimeLimiterEvent>() {
    private val detachedScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val disposeEvent: TimeLimiterEvent
        get() = TimeLimiterEvent.TimeLimiterDisposed

    public suspend fun <T> execute(block: suspend () -> T): T? {
        if (data.timeout == Duration.ZERO) return block()

        return when (data.strategy) {
            TimeoutStrategy.HARD -> executeHard(block)
            TimeoutStrategy.SOFT -> executeSoft(block)
        }
    }

    private suspend fun <T> executeHard(block: suspend () -> T): T? {
        try {
            return withTimeout(data.timeout.inWholeMilliseconds.milliseconds) {
                block()
            }
        } catch (_: TimeoutCancellationException) {
            val ex = TimeoutExceededException(data.timeout.inWholeMilliseconds)
            return handleFallbackOrThrow(ex)
        }
    }

    private suspend fun <T> executeSoft(block: suspend () -> T): T? {
        val deferred = detachedScope.async {
            block()
        }

        try {
            return withTimeout(data.timeout.inWholeMilliseconds.milliseconds) {
                deferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            if (data.cancelOnTimeout) deferred.cancel()
            eventBus.push(TimeLimiterEvent.TimeoutExceeded(data.timeout.inWholeMilliseconds))
            val ex = TimeoutExceededException(data.timeout.inWholeMilliseconds)
            return handleFallbackOrThrow(ex)
        } catch (cancellation: CancellationException) {
            throw cancellation
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> handleFallbackOrThrow(exception: Throwable): T? {
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

    public fun <E : TimeLimiterEvent> subscribe(
        eventType: KClass<E>,
        onError: (suspend (Throwable) -> Unit)? = null,
        on: suspend (E) -> Unit
    ): Job = eventBus.subscribe(eventType, onError, on)

    public inline fun <reified E : TimeLimiterEvent> subscribe(
        noinline onError: (suspend (Throwable) -> Unit)? = null,
        noinline on: suspend (E) -> Unit
    ): Job = subscribe(E::class, onError, on)
}

public fun timeLimiter(builder: TimeLimiterBuilder.() -> Unit): TimeLimiter =
    TimeLimiter(TimeLimiterBuilder().apply(builder).produce())
