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

package com.davils.resilience.circuitbreaker.strategy

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Controls how long a [com.davils.resilience.circuitbreaker.CircuitBreaker] stays in the OPEN state
 * before transitioning to HALF_OPEN.
 *
 * The [attempts] parameter is the number of consecutive OPEN transitions (starts at 1).
 *
 * @since 1.0.0
 */
public fun interface WaitIntervalStrategy {
    /**
     * Returns the wait duration for the given open [attempts] count.
     *
     * @since 1.0.0
     */
    public fun waitDuration(attempts: Int): Duration
}

/**
 * A strategy that always returns the same fixed [duration].
 *
 * @param duration The constant wait time. Must be positive.
 * @since 1.0.0
 */
public fun fixedWaitInterval(duration: Duration): WaitIntervalStrategy =
    WaitIntervalStrategy { duration }

/**
 * A strategy that increases the wait duration exponentially with the number of OPEN attempts.
 *
 * The formula is `initialDuration * multiplier^(attempts - 1)`, capped at [maxDuration].
 *
 * @param initialDuration The wait time on the first OPEN transition.
 * @param multiplier The exponential growth factor (>= 1).
 * @param maxDuration The upper bound on the wait duration.
 * @since 1.0.0
 */
public fun exponentialWaitInterval(
    initialDuration: Duration = 60.seconds,
    multiplier: Double = 2.0,
    maxDuration: Duration = Duration.INFINITE,
): WaitIntervalStrategy {
    require(multiplier >= 1.0) { "multiplier must be >= 1.0" }
    return WaitIntervalStrategy { attempts ->
        val factor = multiplier.pow((attempts - 1).toDouble())
        val computed = initialDuration * factor
        if (maxDuration.isInfinite()) computed else minOf(computed, maxDuration)
    }
}

private fun minOf(a: Duration, b: Duration): Duration = if (a <= b) a else b
