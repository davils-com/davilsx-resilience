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

package com.davils.example.circuitbreaker

import com.davils.resilience.circuitbreaker.CircuitBreakerEvent
import com.davils.resilience.circuitbreaker.CircuitBreakerState
import com.davils.resilience.circuitbreaker.SlidingWindowType
import com.davils.resilience.circuitbreaker.circuitBreaker
import com.davils.resilience.circuitbreaker.exception.CallNotPermittedException
import com.davils.resilience.circuitbreaker.strategy.exponentialWaitInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Simulated remote service — fails the first few calls then recovers.
private var callCount = 0
private suspend fun remoteService(): String {
    callCount++
    delay(5.milliseconds)
    if (callCount <= 6) throw RuntimeException("Service unavailable (call #$callCount)")
    return "OK (call #$callCount)"
}

fun main() = runBlocking {
    val cb = circuitBreaker {
        failureRateThreshold = 50f
        slidingWindowType = SlidingWindowType.COUNT_BASED
        slidingWindowSize = 4
        minimumNumberOfCalls = 4
        permittedCallsInHalfOpenState = 2
        waitDurationInOpenState(300.milliseconds)
        slowCallDurationThreshold = 100.milliseconds
        automaticTransitionFromOpenToHalfOpen = false

        event {
            scope = CoroutineScope(Dispatchers.Default)
        }
    }

    // Subscribe to state-change events for logging
    cb.subscribe<CircuitBreakerEvent.StateTransition> { (from, to) ->
        println("[EVENT] State transition: $from → $to")
    }
    cb.subscribe<CircuitBreakerEvent.FailureRateExceeded> { (rate) ->
        println("[EVENT] Failure rate exceeded: ${"%.1f".format(rate)}%")
    }
    cb.subscribe<CircuitBreakerEvent.CallNotPermitted> {
        println("[EVENT] Call not permitted (state=${it.state})")
    }

    println("=== Circuit Breaker Demo ===")
    println("Window: 4 calls, 50% failure threshold, wait: 300ms, probes: 2\n")

    // Phase 1 — drive the circuit open
    repeat(10) { i ->
        try {
            val result = cb.execute { remoteService() }
            val m = cb.getMetrics()
            println("Call ${i + 1}: $result  [state=${cb.getState()}, failures=${m.numberOfFailedCalls}/${m.numberOfBufferedCalls}]")
        } catch (e: CallNotPermittedException) {
            println("Call ${i + 1}: BLOCKED — circuit is ${e.state}")
        } catch (e: RuntimeException) {
            val m = cb.getMetrics()
            println("Call ${i + 1}: FAILED — ${e.message}  [state=${cb.getState()}, failures=${m.numberOfFailedCalls}/${m.numberOfBufferedCalls}]")
        }
        delay(20.milliseconds)
    }

    // Phase 2 — wait for the open state to expire, then let probes close the circuit
    if (cb.getState() == CircuitBreakerState.OPEN) {
        println("\nCircuit is OPEN. Waiting 400ms for it to become HALF_OPEN on next call…")
        delay(400.milliseconds)
    }

    println("\n=== Probe calls (HALF_OPEN) ===")
    repeat(4) { i ->
        try {
            val result = cb.execute { remoteService() }
            println("Probe ${i + 1}: $result  [state=${cb.getState()}]")
        } catch (e: CallNotPermittedException) {
            println("Probe ${i + 1}: BLOCKED — circuit is ${e.state}")
        } catch (e: RuntimeException) {
            println("Probe ${i + 1}: FAILED — ${e.message}  [state=${cb.getState()}]")
        }
        delay(20.milliseconds)
    }

    println("\n=== Final state: ${cb.getState()} ===")
    println(cb.getMetrics())

    // Phase 3 — demonstrate exponential wait interval
    println("\n=== Exponential wait interval example ===")
    val exponentialCb = circuitBreaker {
        failureRateThreshold = 50f
        slidingWindowSize = 2
        minimumNumberOfCalls = 2
        waitIntervalInOpenState = exponentialWaitInterval(
            initialDuration = 1.seconds,
            multiplier = 2.0,
            maxDuration = 30.seconds
        )
        event { scope = CoroutineScope(Dispatchers.Default) }
    }

    println("Attempt 1 wait: ${exponentialCb.data.waitIntervalInOpenState.waitDuration(1)}")
    println("Attempt 2 wait: ${exponentialCb.data.waitIntervalInOpenState.waitDuration(2)}")
    println("Attempt 3 wait: ${exponentialCb.data.waitIntervalInOpenState.waitDuration(3)}")
    println("Attempt 6 wait: ${exponentialCb.data.waitIntervalInOpenState.waitDuration(6)} (capped at 30s)")
}
