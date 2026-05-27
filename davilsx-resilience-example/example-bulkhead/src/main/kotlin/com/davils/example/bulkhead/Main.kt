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

package com.davils.example.bulkhead

import com.davils.resilience.bulkhead.bulkhead
import com.davils.resilience.bulkhead.event.BulkheadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlin.time.Duration.Companion.milliseconds

suspend fun main() {
    val bulkhead = bulkhead {
        maxConcurrentCalls = 3
        maxWaitDuration = 500.milliseconds

        maxWaitDuration(1000)
        maxWaitDuration(1000.milliseconds)
        maxConcurrentCalls(5)


        event {
            scope = CoroutineScope(Dispatchers.Default)
            replay = 0
            overflowStrategy = BufferOverflow.DROP_OLDEST
            extraBufferCapacity = 128
            onError = { throwable -> println("Error: $throwable") }
        }
    }

    bulkhead.subscribe<BulkheadEvent.BulkheadDispose> {
        // do something when bulkhead is disposed
    }


}
