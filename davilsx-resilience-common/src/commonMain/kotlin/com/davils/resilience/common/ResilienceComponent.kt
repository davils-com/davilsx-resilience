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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public abstract class ResilienceComponent<T : EventMarker> : DisposableAsync {
    protected abstract val disposeEvent: T
    protected abstract val eventBus: EventBus<T>
    private var isDisposed: Boolean = false
    protected val mutex: Mutex = Mutex()

    public suspend fun isDisposed(): Boolean = mutex.withLock { isDisposed }

    override suspend fun dispose() {
        mutex.withLock {
            if (isDisposed) return@withLock
            eventBus.push(disposeEvent)
            isDisposed = true
        }

        eventBus.dispose()
    }
}
