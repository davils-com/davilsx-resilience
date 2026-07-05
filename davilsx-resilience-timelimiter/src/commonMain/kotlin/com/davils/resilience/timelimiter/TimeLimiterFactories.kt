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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a time limiter with the hard timeout strategy.
 *
 * The guarded block runs in the caller's coroutine and is cancelled when the timeout expires.
 *
 * @param timeout Maximum execution duration. Defaults to 1 second.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [TimeLimiter] instance.
 * @since 1.0.0
 */
public fun hardTimeLimiter(
    timeout: Duration = 1.seconds,
    builder: TimeLimiterBuilder.() -> Unit = {},
): TimeLimiter = timeLimiter {
    this.timeout = timeout
    strategy = TimeoutStrategy.HARD
    builder()
}

/**
 * Creates a time limiter with the soft timeout strategy.
 *
 * The guarded block runs in a detached coroutine. When the timeout expires, the caller stops
 * waiting; background work may continue unless [TimeLimiterBuilder.cancelOnTimeout] is enabled.
 *
 * @param timeout Maximum wait duration. Defaults to 1 second.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [TimeLimiter] instance.
 * @since 1.0.0
 */
public fun softTimeLimiter(
    timeout: Duration = 1.seconds,
    builder: TimeLimiterBuilder.() -> Unit = {},
): TimeLimiter = timeLimiter {
    this.timeout = timeout
    strategy = TimeoutStrategy.SOFT
    builder()
}
