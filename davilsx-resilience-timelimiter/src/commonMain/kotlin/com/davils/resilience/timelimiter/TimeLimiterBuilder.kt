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

import com.davils.kore.annotation.KoreDsl
import com.davils.resilience.common.ResilienceComponentBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * DSL builder for configuring a [TimeLimiter].
 *
 * @since 1.0.0
 */
@KoreDsl
public class TimeLimiterBuilder internal constructor() : ResilienceComponentBuilder<TimeLimiterData>() {
    /** Maximum execution duration. Defaults to 1 second. Set to [Duration.ZERO] to disable limiting. */
    public var timeout: Duration = 1.seconds

    /** Whether running work is cancelled when a soft timeout expires. Defaults to `true`. */
    public var cancelOnTimeout: Boolean = true

    /** Timeout strategy. Defaults to [TimeoutStrategy.HARD]. */
    public var strategy: TimeoutStrategy = TimeoutStrategy.HARD

    /** Optional fallback invoked when a timeout occurs. */
    public var fallback: (suspend (Throwable) -> Any?)? = null

    public fun timeout(timeout: Duration) {
        this.timeout = timeout
    }

    public fun cancelOnTimeout(cancel: Boolean) {
        this.cancelOnTimeout = cancel
    }

    public fun strategy(strategy: TimeoutStrategy) {
        this.strategy = strategy
    }

    public fun <T> fallback(fallback: suspend (Throwable) -> T?) {
        this.fallback = fallback
    }

    override fun data(): TimeLimiterData {
        val eventData = eventBuilder.produce()
        return TimeLimiterData(
            timeout = timeout,
            cancelOnTimeout = cancelOnTimeout,
            strategy = strategy,
            fallback = fallback,
            eventData = eventData,
        )
    }
}
