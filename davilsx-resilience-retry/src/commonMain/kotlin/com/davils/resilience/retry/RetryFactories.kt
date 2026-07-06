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

package com.davils.resilience.retry

import com.davils.resilience.retry.strategy.constant.constantBackoff
import com.davils.resilience.retry.strategy.exponential.exponentialBackoff
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates a retry policy with a fixed delay between attempts.
 *
 * @param maxAttempts Maximum attempts including the initial call. Defaults to 3.
 * @param delay Fixed delay between attempts. Defaults to 1000 milliseconds.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [Retry] instance.
 * @since 1.0.0
 */
public fun fixedDelayRetry(
    maxAttempts: Int = 3,
    delay: Duration = 1000.milliseconds,
    builder: RetryBuilder.() -> Unit = {},
): Retry = retry {
    maxAttempts(maxAttempts)
    backoffStrategy(constantBackoff { this.delay = delay })
    builder()
}

/**
 * Creates a retry policy with exponential backoff between attempts.
 *
 * @param maxAttempts Maximum attempts including the initial call. Defaults to 3.
 * @param initialDelay Delay before the first retry. Defaults to 1000 milliseconds.
 * @param multiplier Growth factor applied per attempt. Defaults to 2.0.
 * @param maxDelay Upper bound on the computed delay. Defaults to 60 seconds.
 * @param builder Optional configuration block applied after the preset.
 * @return A new [Retry] instance.
 * @since 1.0.0
 */
public fun exponentialRetry(
    maxAttempts: Int = 3,
    initialDelay: Duration = 1000.milliseconds,
    multiplier: Double = 2.0,
    maxDelay: Duration = 60_000.milliseconds,
    builder: RetryBuilder.() -> Unit = {},
): Retry = retry {
    maxAttempts(maxAttempts)
    backoffStrategy(
        exponentialBackoff {
            this.initialDelay = initialDelay
            this.multiplier = multiplier
            this.maxDelay = maxDelay
        },
    )
    builder()
}
