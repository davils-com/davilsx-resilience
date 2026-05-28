package com.davils.resilience.common.event

import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.plus

/**
 * Builder class for creating instances of [ResilienceEventData].
 *
 * This builder provides a DSL-like API to configure event handling parameters
 * such as coroutine scope, buffer overflow strategy, and replay capacity.
 *
 * @since 1.0.0
 */
public class ResilienceEventBuilder : DslValidator<ResilienceEventData>() {
    /**
     * The coroutine scope used for event processing.
     *
     * Defaults to [Dispatchers.Default].
     *
     * @since 1.0.0
     */
    public var scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    /**
     * The strategy to use when the event buffer overflows.
     *
     * Defaults to [BufferOverflow.DROP_OLDEST].
     *
     * @since 1.0.0
     */
    public var overflowStrategy: BufferOverflow = BufferOverflow.DROP_OLDEST

    /**
     * The number of values replayed to new subscribers.
     *
     * Defaults to 0. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var replay: Int = 0

    /**
     * The additional capacity for the event buffer.
     *
     * Defaults to 128. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var extraBufferCapacity: Int = 128

    /**
     * A suspending callback triggered when an error occurs during event processing.
     *
     * Defaults to an empty callback.
     *
     * @since 1.0.0
     */
    public var onError: suspend (Throwable) -> Unit = {}

    /**
     * Creates a new instance of [ResilienceEventData] with the configured parameters.
     *
     * A [SupervisorJob] is added to the provided [scope] to ensure that failures
     * in event processing do not cancel the parent scope.
     *
     * @return A configured [ResilienceEventData] instance.
     * @since 1.0.0
     */
    override fun data(): ResilienceEventData {
        val coroutineScope = scope + SupervisorJob()
        return ResilienceEventData(
            scope = coroutineScope,
            onError = onError,
            replay = replay,
            overflowStrategy = overflowStrategy,
            extraBufferCapacity = extraBufferCapacity
        )
    }
}
