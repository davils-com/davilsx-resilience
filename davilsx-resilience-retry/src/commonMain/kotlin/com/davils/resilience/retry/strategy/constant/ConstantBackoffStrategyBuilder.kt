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

import com.davils.kore.annotation.KoreDsl
import com.davils.kore.pattern.dsl.validation.DslValidator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A builder class for creating instances of [ConstantBackoffStrategyData].
 *
 * This builder provides a DSL-friendly way to configure the delay for a constant
 * backoff strategy. It ensures that the provided delay is non-negative.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ConstantBackoffStrategyBuilder internal constructor() : DslValidator<ConstantBackoffStrategyData>() {
    /**
     * The fixed delay duration between retry attempts.
     *
     * Defaults to 1000 milliseconds. Must be non-negative.
     *
     * @since 1.0.0
     */
    public var delay: Duration = 1000.milliseconds

    /**
     * Sets the fixed delay duration between retry attempts.
     *
     * @param delay The duration to wait between attempts.
     * @since 1.0.0
     */
    public fun delay(delay: Duration) {
        this.delay = delay
    }

    /**
     * Sets the fixed delay duration between retry attempts in milliseconds.
     *
     * @param delayMillis The duration in milliseconds to wait between attempts.
     * @since 1.0.0
     */
    public fun delay(delayMillis: Long) {
        this.delay = delayMillis.milliseconds
    }

    override fun data(): ConstantBackoffStrategyData = ConstantBackoffStrategyData(delay)
}