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

import com.davils.kore.pattern.reactive.event.EventMarker
import com.davils.resilience.common.event.ResilienceEvent
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration

/**
 * Base class for all events emitted by a [com.davils.resilience.retry.Retry] instance.
 *
 * Events are pushed to the internal event bus during various stages of the retry
 * execution lifecycle, allowing for monitoring, logging, or custom side effects.
 *
 * @since 1.0.0
 */
public sealed class RetryEvent : EventMarker() {
    /**
     * Emitted when a single retry attempt has failed with an exception.
     *
     * This event does not necessarily mean the entire retry operation has failed,
     * as further attempts might be scheduled if the predicate allows.
     *
     * @param attempt The index of the attempt that failed (starting at 1).
     * @param throwable The exception that caused the attempt to fail.
     * @since 1.0.0
     */
    public data class RetryAttemptFailed(public val attempt: Int, public val throwable: Throwable) : RetryEvent()

    /**
     * Emitted when a new retry attempt is about to be executed.
     *
     * @param attempt The index of the attempt that is starting (starting at 1).
     * @since 1.0.0
     */
    public data class RetryAttemptStarted(public val attempt: Int) : RetryEvent()

    /**
     * Emitted when a retry attempt has succeeded.
     *
     * This event indicates the successful completion of the operation and signals
     * that no further retries will be performed for the current execution.
     *
     * @param attempt The index of the attempt that succeeded.
     * @since 1.0.0
     */
    public data class RetrySucceeded(public val attempt: Int) : RetryEvent()

    /**
     * Emitted when the entire retry operation has failed after exhausting all attempts or matching an ignore condition.
     *
     * @param attempt The total number of attempts performed.
     * @param throwable The final exception that caused the failure.
     * @since 1.0.0
     */
    public data class RetryFailed(public val attempt: Int, public val throwable: Throwable) : RetryEvent()

    /**
     * Emitted when the operation is cancelled via [CancellationException].
     *
     * @param attempt The attempt index during which the cancellation occurred.
     * @param cancellation The exception describing the cancellation.
     * @since 1.0.0
     */
    public data class RetryCancelled(
        public val attempt: Int,
        public val cancellation: CancellationException
    ) : RetryEvent()

    /**
     * Emitted when the retry instance is disposed.
     *
     * After this event, no further events will be emitted and the event bus is closed.
     *
     * @since 1.0.0
     */
    public data object RetryDisposed : RetryEvent()

    /**
     * Emitted when a retry attempt has failed and the system is waiting for the next backoff delay.
     *
     * @param attempt The index of the attempt that just failed.
     * @param delay The duration to wait before the next attempt starts.
     * @since 1.0.0
     */
    public data class RetryAttemptBackoff(public val attempt: Int, public val delay: Duration) : RetryEvent()
}
