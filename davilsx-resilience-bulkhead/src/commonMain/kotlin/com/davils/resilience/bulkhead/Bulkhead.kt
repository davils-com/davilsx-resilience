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

import com.davils.resilience.bulkhead.exception.BulkheadMaxConcurrentCallsException
import com.davils.resilience.common.ResilienceComponent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * A bulkhead resilience component that limits the number of concurrent calls.
 *
 * The [Bulkhead] uses semaphore to ensure that no more than [BulkheadData.maxConcurrentCalls]
 * are executed at the same time. If the limit is reached, callers will wait up to
 * [BulkheadData.maxWaitDuration] before a [BulkheadMaxConcurrentCallsException] is thrown.
 *
 * This component helps to isolate failures and prevent resource exhaustion in one part
 * of the system from affecting others.
 *
 * @since 1.0.0
 */
public class Bulkhead internal constructor(
    override val data: BulkheadData
) : ResilienceComponent<BulkheadData, BulkheadEvent>() {
    override val disposeEvent: BulkheadEvent
        get() = BulkheadEvent.BulkheadDispose

    private val semaphore = Semaphore(permits = data.maxConcurrentCalls)

    private val permits = atomic(0)

    private suspend fun tryAcquireWithTimeout(): Boolean {
        if (data.maxWaitDuration == Duration.ZERO){
            val acquired = semaphore.tryAcquire()
            if (!acquired) {
                eventBus.push(BulkheadEvent.BulkheadCallRejected(data.maxConcurrentCalls))
            }
            return acquired
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

    /**
     * Executes the given [block] within the bulkhead.
     *
     * Limits the number of concurrent executions of the block. If the limit is reached,
     * it waits for a permit up to the configured maximum wait duration.
     *
     * @param T The return type of the block.
     * @param block The suspendable operation to execute.
     * @return The result of the block execution.
     * @throws BulkheadMaxConcurrentCallsException If the concurrent call limit is exceeded.
     * @throws CancellationException If the operation is cancelled.
     * @since 1.0.0
     */
    public suspend fun <T> execute(block: suspend () -> T): T {
        val startMark = TimeSource.Monotonic.markNow()
        val acquired = tryAcquireWithTimeout()
        val waitDuration = startMark.elapsedNow()

        eventBus.push(BulkheadEvent.BulkheadPermitWaited(waitDuration))

        if (!acquired) {
            throw BulkheadMaxConcurrentCallsException("Bulkhead max concurrent calls limit of ${data.maxConcurrentCalls} exceeded")
        }

        return try {
            val currentPermits = permits.incrementAndGet()
            eventBus.push(BulkheadEvent.BulkheadPermitAcquired(currentPermits))
            val result = block()
            eventBus.push(BulkheadEvent.BulkheadOnSuccess)
            result
        } catch (t: Throwable) {
            if (t !is CancellationException) {
                eventBus.push(BulkheadEvent.BulkheadOnError(t))
            }
            throw t
        } finally {
            semaphore.release()
            val currentPermits = permits.decrementAndGet()
            eventBus.push(BulkheadEvent.BulkheadPermitReleased(currentPermits))
        }
    }
}

/**
 * Creates a new [Bulkhead] instance using the provided configuration [builder].
 *
 * @param builder The configuration builder block.
 * @return A new [Bulkhead] instance.
 * @since 1.0.0
 */
public fun bulkhead(builder: BulkheadBuilder.() -> Unit): Bulkhead {
    val bulkheadBuilder = BulkheadBuilder()
    bulkheadBuilder.apply(builder)
    val data = bulkheadBuilder.produce()
    return Bulkhead(data)
}
