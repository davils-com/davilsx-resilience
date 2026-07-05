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

package com.davils.example.timelimiter

import com.davils.resilience.timelimiter.TimeLimiterEvent
import com.davils.resilience.timelimiter.hardTimeLimiter
import com.davils.resilience.timelimiter.softTimeLimiter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

fun main() = runBlocking {
    println("=== Hard time limiter ===")
    val hard = hardTimeLimiter(200.milliseconds)

    runCatching {
        hard.execute {
            delay(50)
            "fast"
        }
    }.onSuccess { println("  ok: $it") }
        .onFailure { println("  failed: ${it.message}") }

    runCatching {
        hard.execute {
            delay(500)
            "slow"
        }
    }.onSuccess { println("  ok: $it") }
        .onFailure { println("  failed: ${it.message}") }

    println("\n=== Soft time limiter with fallback ===")
    val soft = softTimeLimiter(200.milliseconds) {
        fallback<String> { "fallback-value" }
    }

    val result = soft.execute {
        delay(500)
        "slow"
    }
    println("  result: $result")

    println("\n=== Events ===")
    val events = Channel<TimeLimiterEvent.TimeoutExceeded>(10)
    val limiter = hardTimeLimiter(100.milliseconds)
    val job = launch {
        limiter.subscribe(TimeLimiterEvent.TimeoutExceeded::class) { events.send(it) }
    }

    runCatching { limiter.execute { delay(300) } }
    withTimeout(500.milliseconds) {
        val event = events.receive()
        println("  timeout event: ${event.timeoutMs}ms")
    }
    job.cancel()

    println("\n=== Metrics ===")
    val metrics = hard.getMetrics()
    println("  successful=${metrics.numberOfSuccessfulCalls}, timeouts=${metrics.numberOfTimeoutCalls}")
}
