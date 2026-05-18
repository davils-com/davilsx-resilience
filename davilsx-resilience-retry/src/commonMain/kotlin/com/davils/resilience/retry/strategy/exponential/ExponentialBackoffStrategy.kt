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

package com.davils.resilience.retry.strategy.exponential

import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.math.pow
import kotlin.time.Duration

/**
 * A backoff strategy that increases the delay exponentially with each retry attempt.
 *
 * The delay is calculated using the formula: `initialDelay * multiplier ^ (attempt - 1)`.
 * The resulting delay is capped at a maximum value defined in the strategy data.
 *
 * This strategy is effective for handling temporary failures in distributed systems
 * by increasing the wait time to allow the system to recover.
 *
 * @since 1.0.0
 */
public class ExponentialBackoffStrategy internal constructor(
    private val data: ExponentialBackoffStrategyData
) : BackoffStrategy {
    /**
     * Calculates the exponential delay for the given retry attempt.
     *
     * @param attempt The current retry attempt number.
     * @return The duration to wait before the next attempt, capped at [ExponentialBackoffStrategyData.maxDelay].
     * @since 1.0.0
     */
    override fun calculateDelay(attempt: Int): Duration {
        val safeAttempt = maxOf(attempt, 1)
        val delay = data.initialDelay * data.multiplier.pow(safeAttempt - 1)
        return minOf(delay, data.maxDelay)
    }
}

/**
 * Creates a [BackoffStrategy] with an exponential delay.
 *
 * This function provides a DSL-like interface to configure and instantiate
 * an [ExponentialBackoffStrategy].
 *
 * @param builder A lambda to configure the [ExponentialBackoffStrategyBuilder].
 * @return A [BackoffStrategy] that uses an exponential delay.
 * @since 1.0.0
 */
public fun exponentialBackoff(builder: ExponentialBackoffStrategyBuilder.() -> Unit = {}): BackoffStrategy {
    val exponentialBuilder = ExponentialBackoffStrategyBuilder()
    exponentialBuilder.builder()
    val data = exponentialBuilder.produce()
    return ExponentialBackoffStrategy(data)
}
