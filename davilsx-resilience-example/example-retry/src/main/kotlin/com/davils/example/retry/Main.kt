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

package com.davils.example.retry

import com.davils.resilience.retry.exponentialRetry
import com.davils.resilience.retry.fixedDelayRetry
import com.davils.resilience.retry.predicate.throwable.throwablePredicate
import com.davils.resilience.retry.strategy.constant.constantBackoff
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

fun main() = runBlocking {
    println("=== Fixed delay retry ===")
    val fixed = fixedDelayRetry(maxAttempts = 3, delay = 50.milliseconds) {
        backoffStrategy(constantBackoff { delay(0L) })
        predicate = throwablePredicate { throwable(RuntimeException::class) }
    }

    var calls = 0
    val result = fixed.execute {
        calls++
        if (calls < 3) throw RuntimeException("transient-$calls")
        "success-after-$calls-attempts"
    }
    println("  result=$result")

    println("\n=== Exponential retry ===")
    val exponential = exponentialRetry(maxAttempts = 4, initialDelay = 10.milliseconds) {
        backoffStrategy(constantBackoff { delay(0L) })
    }
    exponential.execute { "ok" }
    println("  exponential policy executed successfully")

    println("\n=== Metrics ===")
    val metrics = fixed.getMetrics()
    println("  totalCalls=${metrics.totalCalls}, successful=${metrics.successfulCalls}")
    println("  totalAttempts=${metrics.totalAttempts}, failedAttempts=${metrics.failedAttempts}")
}
