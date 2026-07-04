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

package com.davils.resilience.common

import com.davils.kore.pattern.functional.loan.DisposableAsync
import com.davils.kore.pattern.reactive.event.EventBus
import com.davils.kore.pattern.reactive.event.EventMarker
import com.davils.kore.pattern.reactive.event.EventTopic
import com.davils.kore.pattern.reactive.event.eventBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Base class for all resilience components.
 *
 * This class provides common functionality for resilience components, including
 * event handling through an [EventBus] and lifecycle management via [DisposableAsync].
 *
 * @param D The type of configuration data for this component.
 * @param E The type of event marker used by this component.
 * @since 1.0.0
 */
public abstract class ResilienceComponent<D : ResilienceComponentData, E : EventMarker> : DisposableAsync {
    private var isDisposed: Boolean = false

    /**
     * The configuration data for this component.
     *
     * @since 1.0.0
     */
    protected abstract val data: D

    /**
     * The event triggered when the component is disposed.
     *
     * @since 1.0.0
     */
    protected abstract val disposeEvent: E

    /**
     * Mutex used to ensure thread-safe access to the component's state.
     *
     * @since 1.0.0
     */
    protected val mutex: Mutex = Mutex()

    /**
     * The backing event bus that owns this component's single event topic.
     *
     * Initialized lazily because it depends on [data], which is provided by subclasses
     * and is therefore not available while the base class is being constructed.
     *
     * @since 1.0.0
     */
    private val bus: EventBus by lazy {
        eventBus(data.eventData.scope) {
            replay = data.eventData.replay
            onError = data.eventData.onError
            overflowStrategy = data.eventData.overflowStrategy
            extraBufferCapacity = data.eventData.extraBufferCapacity
            topic<EventMarker>(EVENT_TOPIC_NAME)
        }
    }

    /**
     * The event topic used for publishing and subscribing to component events.
     *
     * All component events flow through this single topic. Events are published as
     * [EventMarker] instances; type-specific delivery is handled by [subscribe].
     *
     * @since 1.0.0
     */
    protected val eventBus: EventTopic<EventMarker> by lazy { bus.topic(EVENT_TOPIC_NAME) }

    private fun isDisposedUnsafe(): Boolean = isDisposed

    /**
     * Disposes of the component and its resources.
     *
     * This method is thread-safe. It publishes the [disposeEvent] and closes the underlying bus.
     *
     * @since 1.0.0
     */
    override suspend fun dispose() {
        mutex.withLock {
            if (isDisposed) return@withLock
            eventBus.push(disposeEvent)
            isDisposed = true
        }

        bus.dispose()
    }

    /**
     * Checks whether the component has been disposed.
     *
     * @return `true` if the component is disposed, `false` otherwise.
     * @since 1.0.0
     */
    public suspend fun isDisposed(): Boolean = mutex.withLock { isDisposedUnsafe() }

    /**
     * Subscribes to events of a specific type.
     *
     * @param R The type of event to subscribe to.
     * @param eventType The class of the event type.
     * @param onError An optional callback triggered when an error occurs during event processing.
     * @param on The callback triggered when an event is received.
     * @return A [Job] representing the subscription.
     * @since 1.0.0
     */
    public fun <R : EventMarker> subscribe(
        eventType: KClass<R>,
        onError: (suspend (Throwable) -> Unit)? = null,
        on: suspend (R) -> Unit
    ): Job = eventBus.subscribe(onError) { event ->
        eventType.safeCast(event)?.let { on(it) }
    }

    /**
     * Subscribes to events of a specific type (reified).
     *
     * @param R The type of event to subscribe to.
     * @param onError An optional callback triggered when an error occurs during event processing.
     * @param on The callback triggered when an event is received.
     * @return A [Job] representing the subscription.
     * @since 1.0.0
     */
    public inline fun <reified R : EventMarker> subscribe(
        noinline onError: (suspend (Throwable) -> Unit)? = null,
        noinline on: suspend (R) -> Unit
    ): Job = subscribe(R::class, onError, on)

    private companion object {
        private const val EVENT_TOPIC_NAME = "events"
    }
}
