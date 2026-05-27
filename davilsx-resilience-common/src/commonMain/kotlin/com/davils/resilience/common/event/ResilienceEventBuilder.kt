package com.davils.resilience.common.event

import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.plus

public class ResilienceEventBuilder : DslValidator<ResilienceEventData>() {
    public var scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    public var overflowStrategy: BufferOverflow = BufferOverflow.DROP_OLDEST
    public var replay: Int = 0
    public var extraBufferCapacity: Int = 128
    public var onError: suspend (Throwable) -> Unit = {}

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
