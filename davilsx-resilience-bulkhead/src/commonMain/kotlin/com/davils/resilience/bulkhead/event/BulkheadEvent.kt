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

package com.davils.resilience.bulkhead.event

import com.davils.kore.pattern.reactive.event.EventMarker
import kotlin.time.Duration

/**
 * Base class for all events emitted by a [com.davils.resilience.bulkhead.Bulkhead] instance.
 *
 * @since 1.0.0
 */
public sealed class BulkheadEvent : EventMarker() {
    /**
     * Emitted when the bulkhead instance is disposed.
     *
     * @since 1.0.0
     */
    public data object BulkheadDispose : BulkheadEvent()

    /**
     * Emitted when a call is rejected because the maximum number of concurrent calls is exceeded.
     *
     * @param maxConcurrentCalls The maximum number of concurrent calls configured.
     * @since 1.0.0
     */
    public data class BulkheadCallRejected(public val maxConcurrentCalls: Int) : BulkheadEvent()

    /**
     * Emitted when a permit is successfully acquired.
     *
     * @param permits The current number of active permits.
     * @since 1.0.0
     */
    public data class BulkheadPermitAcquired(public val permits: Int) : BulkheadEvent()

    /**
     * Emitted when a permit is released.
     *
     * @param permits The current number of active permits after release.
     * @since 1.0.0
     */
    public data class BulkheadPermitReleased(public val permits: Int) : BulkheadEvent()

    /**
     * Emitted to report the duration spent waiting for a permit.
     *
     * @param waitDuration The actual duration spent waiting.
     * @since 1.0.0
     */
    public data class BulkheadPermitWaited(public val waitDuration: Duration) : BulkheadEvent()

    /**
     * Emitted when a call finished successfully.
     *
     * @since 1.0.0
     */
    public data object BulkheadOnSuccess : BulkheadEvent()

    /**
     * Emitted when a call finished with an error.
     *
     * @param throwable The exception that occurred during execution.
     * @since 1.0.0
     */
    public data class BulkheadOnError(public val throwable: Throwable) : BulkheadEvent()
}
