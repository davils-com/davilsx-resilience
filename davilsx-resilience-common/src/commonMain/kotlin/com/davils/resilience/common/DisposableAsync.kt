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

import com.davils.kore.pattern.event.EventBus
import com.davils.resilience.common.event.ResilienceEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public abstract class DisposableAsync<T : ResilienceEvent> {
    protected abstract val eventBus: EventBus<T>
    protected abstract val disposedEvent : T
    protected val mutex: Mutex = Mutex()
    protected var isDisposed: Boolean = false

    public suspend fun dispose() {
        mutex.withLock {
            if (isDisposed) return
            eventBus.push(disposedEvent)

            isDisposed = true
            eventBus.dispose()
        }
    }
}
