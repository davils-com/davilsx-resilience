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

package com.davils.resilience.bulkhead

import com.davils.resilience.bulkhead.event.BulkheadEvent
import com.davils.resilience.bulkhead.exception.BulkheadMaxConcurrentCallsException
import com.davils.resilience.common.ResilienceComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

public class Bulkhead internal constructor(
    override val data: BulkheadData
) : ResilienceComponent<BulkheadData, BulkheadEvent>() {
    override val disposeEvent: BulkheadEvent
        get() = BulkheadEvent.BulkheadDispose

    private val semaphore = Semaphore(permits = data.maxConcurrentCalls)

    public suspend fun <T> execute(block: suspend () -> T): T {
        val acquired = tryAcquireWithTimeout()
        if (!acquired) {
            throw BulkheadMaxConcurrentCallsException("Bulkhead max concurrent calls limit of ${data.maxConcurrentCalls} exceeded")
        }

        return try {
            block()
        } finally {
            semaphore.release()
        }
    }

    private suspend fun tryAcquireWithTimeout(): Boolean {
        if (data.maxWaitDuration == Duration.ZERO){
            return semaphore.tryAcquire()
        }

        return try {
            withTimeout(data.maxWaitDuration) {
                semaphore.acquire()
                true
            }
        } catch (_: TimeoutCancellationException) {
            false
        } catch (_: CancellationException) {
            false
        }
    }
}

public fun bulkhead(builder: BulkheadBuilder.() -> Unit): Bulkhead {
    val bulkheadBuilder = BulkheadBuilder()
    bulkheadBuilder.apply(builder)
    val data = bulkheadBuilder.produce()
    return Bulkhead(data)
}
