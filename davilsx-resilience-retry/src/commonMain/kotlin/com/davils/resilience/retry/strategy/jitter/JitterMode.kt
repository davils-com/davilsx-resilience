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

package com.davils.resilience.retry.strategy.jitter

/**
 * Defines the algorithm used to apply jitter to a base backoff delay.
 *
 * Jitter helps to avoid the "thundering herd" problem where many clients retry
 * at exactly the same time, potentially overwhelming the system. Different jitter
 * algorithms provide different trade-offs between randomness, predictability and
 * load distribution.
 *
 * The algorithms are inspired by the AWS Architecture Blog article
 * "Exponential Backoff And Jitter".
 *
 * @since 1.0.0
 */
public enum class JitterMode {
    /**
     * Applies a symmetric proportional jitter around the base delay.
     *
     * The resulting delay is calculated as:
     * `baseDelay * random(1 - factor, 1 + factor)`.
     *
     * This is the legacy behavior and keeps the average delay equal to the base delay
     * while allowing both shorter and longer waits.
     *
     * @since 1.0.0
     */
    PROPORTIONAL,

    /**
     * Applies full jitter to the base delay.
     *
     * The resulting delay is a uniformly distributed random value in the range
     * `[0, baseDelay]`. This algorithm provides the strongest spread of retries
     * across the time window and is recommended for most high-contention scenarios.
     *
     * @since 1.0.0
     */
    FULL,

    /**
     * Applies equal jitter to the base delay.
     *
     * The resulting delay is calculated as:
     * `baseDelay / 2 + random(0, baseDelay / 2)`.
     *
     * Half of the delay is fixed and the other half is randomized, providing a
     * compromise between predictability and randomness.
     *
     * @since 1.0.0
     */
    EQUAL,

    /**
     * Applies decorrelated jitter based on the previously used delay.
     *
     * The resulting delay is calculated as:
     * `min(cap, random(baseDelay, previousDelay * 3))`.
     *
     * This algorithm increases the delay non-monotonically and tends to spread retries
     * very effectively while still bounding them by a configurable cap. The previous
     * delay is tracked internally by the strategy.
     *
     * @since 1.0.0
     */
    DECORRELATED
}
