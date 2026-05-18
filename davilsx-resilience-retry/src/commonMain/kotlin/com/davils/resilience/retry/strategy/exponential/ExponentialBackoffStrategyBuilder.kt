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

import com.davils.kore.annotation.KoreDsl
import com.davils.kore.pattern.dsl.validation.DslValidator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A builder class for creating instances of [ExponentialBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure parameters for an
 * exponential backoff strategy, such as initial delay, multiplier, and maximum delay.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ExponentialBackoffStrategyBuilder internal constructor() : DslValidator<ExponentialBackoffStrategyData>() {
    /**
     * The maximum duration to wait between retry attempts.
     *
     * Defaults to 60,000 milliseconds (1 minute). Must be non-negative.
     *
     * @since 1.0.0
     */
    public var maxDelay: Duration = 60000.milliseconds

    /**
     * The factor by which the delay increases with each attempt.
     *
     * Defaults to 2.0. Must be greater than 0.
     *
     * @since 1.0.0
     */
    public var multiplier: Double = 2.0

    /**
     * The initial duration to wait before the first retry attempt.
     *
     * Defaults to 1000 milliseconds. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var initialDelay: Duration = 1000.milliseconds

    /**
     * Sets the maximum duration to wait between retry attempts.
     *
     * @param maxDelay The maximum duration.
     * @since 1.0.0
     */
    public fun maxDelay(maxDelay: Duration) {
        this.maxDelay = maxDelay
    }

    /**
     * Sets the maximum duration to wait between retry attempts in milliseconds.
     *
     * @param maxDelayMillis The maximum duration in milliseconds.
     * @since 1.0.0
     */
    public fun maxDelay(maxDelayMillis: Long) {
        this.maxDelay = maxDelayMillis.milliseconds
    }

    /**
     * Sets the factor by which the delay increases with each attempt.
     *
     * @param multiplier The growth factor.
     * @since 1.0.0
     */
    public fun multiplier(multiplier: Double) {
        this.multiplier = multiplier
    }

    /**
     * Sets the initial duration to wait before the first retry attempt.
     *
     * @param initialDelay The initial duration.
     * @since 1.0.0
     */
    public fun initialDelay(initialDelay: Duration) {
        this.initialDelay = initialDelay
    }

    /**
     * Sets the initial duration to wait before the first retry attempt in milliseconds.
     *
     * @param initialDelayMillis The initial duration in milliseconds.
     * @since 1.0.0
     */
    public fun initialDelay(initialDelayMillis: Long) {
        this.initialDelay = initialDelayMillis.milliseconds
    }

    override fun data(): ExponentialBackoffStrategyData = ExponentialBackoffStrategyData(
        maxDelay = maxDelay,
        multiplier = multiplier,
        initialDelay = initialDelay
    )
}
