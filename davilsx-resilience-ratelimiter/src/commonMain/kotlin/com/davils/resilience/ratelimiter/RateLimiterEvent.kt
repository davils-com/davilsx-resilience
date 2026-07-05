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

package com.davils.resilience.ratelimiter

import com.davils.kore.pattern.reactive.event.EventMarker
import kotlin.time.Duration

/**
 * Events emitted by a [RateLimiter].
 *
 * @since 1.0.0
 */
public sealed class RateLimiterEvent : EventMarker() {
    /** Emitted when permits were successfully acquired. */
    public data class SuccessfulAcquire(public val permits: Int) : RateLimiterEvent()

    /**
     * Emitted when permits could not be acquired.
     *
     * @property waitDuration The wait time that would have been required to acquire the permits.
     */
    public data class FailedAcquire(public val permits: Int, public val waitDuration: Duration) : RateLimiterEvent()

    /** Emitted when the rate limiter is disposed. */
    public data object RateLimiterDisposed : RateLimiterEvent()
}
