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

package com.davils.example.ratelimiter

import com.davils.resilience.ratelimiter.RateLimiterStrategy
import com.davils.resilience.ratelimiter.fixedRateLimiter
import com.davils.resilience.ratelimiter.slidingWindowRateLimiter
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

fun main() = runBlocking {
    println("=== Fixed window rate limiter ===")
    val fixed = fixedRateLimiter(limit = 3, period = 500.milliseconds) {
        timeoutDuration = 200.milliseconds
        strategy = RateLimiterStrategy.WAIT
    }

    repeat(5) { attempt ->
        runCatching {
            fixed.execute { "request-${attempt + 1}" }
        }.onSuccess { println("  ok: $it") }
            .onFailure { println("  rejected: ${it.message}") }
    }

    println("\n=== Sliding window rate limiter ===")
    val sliding = slidingWindowRateLimiter(limit = 2, period = 300.milliseconds) {
        strategy = RateLimiterStrategy.FAIL_FAST
    }

    repeat(4) {
        val acquired = sliding.tryAcquire()
        println("  tryAcquire -> $acquired (available=${sliding.getAvailableSlots()})")
    }

    println("\n=== Metrics ===")
    val metrics = fixed.getMetrics()
    println("  successful=${metrics.numberOfSuccessfulAcquires}, failed=${metrics.numberOfFailedAcquires}")
    println("  available=${metrics.availablePermissions}, totalWait=${metrics.totalWaitTime}")
}
