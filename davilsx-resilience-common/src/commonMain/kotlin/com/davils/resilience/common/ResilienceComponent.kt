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
import com.davils.kore.pattern.reactive.event.eventBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

public abstract class ResilienceComponent<D : ResilienceComponentData, E : EventMarker> : DisposableAsync {
    private var isDisposed: Boolean = false
    protected abstract val data: D
    protected abstract val disposeEvent: E
    protected val mutex: Mutex = Mutex()
    protected val eventBus: EventBus<E> = eventBus(data.eventData.scope) {
        replay = data.eventData.replay
        onError = data.eventData.onError
        overflowStrategy = data.eventData.overflowStrategy
        extraBufferCapacity = data.eventData.extraBufferCapacity
    }

    protected fun isDisposedUnsafe(): Boolean = isDisposed

    public suspend fun isDisposed(): Boolean = mutex.withLock { isDisposedUnsafe() }

    override suspend fun dispose() {
        mutex.withLock {
            if (isDisposed) return@withLock
            eventBus.push(disposeEvent)
            isDisposed = true
        }

        eventBus.dispose()
    }

    public fun <R : EventMarker> subscribe(
        eventType: KClass<R>,
        onError: (suspend (Throwable) -> Unit)? = null,
        on: suspend (R) -> Unit
    ): Job = eventBus.subscribe(eventType, onError, on)

    public inline fun <reified R : EventMarker> subscribe(
        noinline onError: (suspend (Throwable) -> Unit)? = null,
        noinline on: suspend (R) -> Unit
    ): Job = subscribe(R::class, onError, on)
}
