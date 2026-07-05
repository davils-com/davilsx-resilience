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

package com.davils.resilience.timelimiter

import com.davils.kore.pattern.reactive.event.EventMarker

/**
 * Events emitted by a [TimeLimiter].
 *
 * @since 1.0.0
 */
public sealed class TimeLimiterEvent : EventMarker() {
    /** Emitted when an execution exceeds the configured timeout. */
    public data class TimeoutExceeded(public val timeoutMs: Long) : TimeLimiterEvent()

    /** Emitted when the time limiter is disposed. */
    public data object TimeLimiterDisposed : TimeLimiterEvent()
}
