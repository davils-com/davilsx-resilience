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

package com.davils.resilience.retry.event

import com.davils.kore.annotation.KoreDsl
import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import com.davils.kore.pattern.dsl.validation.DslValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.plus

/**
 * A builder class for configuring the event system of a [com.davils.resilience.retry.Retry] instance.
 *
 * This builder allows customization of the [CoroutineScope] used for event emission,
 * as well as buffer strategies and error handling for the internal event bus.
 *
 * @since 1.0.0
 */
@KoreDsl
public class RetryEventBuilder internal constructor() : DslValidator<RetryEventData>() {
    /**
     * The [CoroutineScope] in which events are emitted and collected.
     *
     * Defaults to [Dispatchers.Default]. A [SupervisorJob] is automatically
     * attached to this scope during construction to ensure failure isolation.
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
     * The number of events to replay to new subscribers.
     *
     * Defaults to 0. This value determines how many events to replay to new subscribers
     * when they subscribe to the event bus.
     *
     * @since 1.0.0
     */
    public var replay: Int = 0

    /**
     * The additional buffer capacity for the event bus.
     *
     * Defaults to 128. This value determines how many events can be buffered
     * beyond the [replay] count before the [overflowStrategy] kicks in.
     *
     * @since 1.0.0
     */
    public var extraBufferCapacity: Int = 128

    /**
     * A callback invoked when an error occurs during event processing or emission.
     *
     * Defaults to an empty lambda.
     *
     * @since 1.0.0
     */
    public var onError: suspend (Throwable) -> Unit = {}

    /**
     * Produces a [RetryEventData] instance based on the current configuration.
     *
     * @return A validated [RetryEventData] instance.
     * @since 1.0.0
     */
    override fun data(): RetryEventData {
        val coroutineScope = scope + SupervisorJob()
        return RetryEventData(
            onError = onError,
            scope = coroutineScope,
            overflowStrategy = overflowStrategy,
            replay = replay,
            extraBufferCapacity = extraBufferCapacity
        )
    }
}
