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

package com.davils.resilience.retry.strategy.constant

import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.time.Duration

/**
 * A backoff strategy that uses a fixed delay between retry attempts.
 *
 * This strategy always returns the same delay duration regardless of the attempt number.
 * It is suitable for simple retry scenarios where the overhead of recalculating
 * delays or increasing wait times is not required.
 *
 * @since 1.0.0
 */
public class ConstantBackoffStrategy internal constructor(private val data: ConstantBackoffStrategyData): BackoffStrategy {
    /**
     * Returns the fixed delay duration defined in the strategy data.
     *
     * @param attempt The current retry attempt number (ignored in this strategy).
     * @return The fixed duration to wait before the next attempt.
     * @since 1.0.0
     */
    override fun calculateDelay(attempt: Int): Duration = data.delay
}

/**
 * Creates a [BackoffStrategy] with a constant delay.
 *
 * This function provides a DSL-like interface to configure and instantiate
 * a [ConstantBackoffStrategy].
 *
 * @param builder A lambda to configure the [ConstantBackoffStrategyBuilder].
 * @return A [BackoffStrategy] that uses a constant delay.
 * @since 1.0.0
 */
public fun constantBackoff(builder: ConstantBackoffStrategyBuilder.() -> Unit = {}): BackoffStrategy {
    val constantBuilder = ConstantBackoffStrategyBuilder()
    constantBuilder.builder()
    val data = constantBuilder.produce()
    return ConstantBackoffStrategy(data)
}
