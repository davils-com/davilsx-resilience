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

package com.davils.resilience.circuitbreaker.internal

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Common interface for COUNT_BASED and TIME_BASED sliding window implementations.
 *
 * All implementations are NOT thread-safe on their own — callers must hold the
 * component's mutex before invoking any method.
 */
internal interface SlidingWindowMetrics {
    /** Record a completed call. Must be called under the component mutex. */
    fun record(outcome: CallOutcome, duration: Duration)

    /** Returns a consistent snapshot. Must be called under the component mutex. */
    fun snapshot(): WindowSnapshot

    /** Resets all recorded data. Must be called under the component mutex. */
    fun reset()
}

// ---------------------------------------------------------------------------
// COUNT_BASED — fixed-size ring buffer
// ---------------------------------------------------------------------------

internal class CountBasedSlidingWindow(private val size: Int) : SlidingWindowMetrics {
    private val outcomes = Array<CallOutcome?>(size) { null }
    private var head = 0
    private var count = 0

    // Running totals for O(1) snapshot
    private var successCount = 0
    private var errorCount = 0
    private var slowSuccessCount = 0
    private var slowErrorCount = 0

    override fun record(outcome: CallOutcome, duration: Duration) {
        val evicted = outcomes[head]
        evicted?.let { subtractFromTotals(it) }
        outcomes[head] = outcome
        addToTotals(outcome)
        head = (head + 1) % size
        if (count < size) count++
    }

    override fun snapshot(): WindowSnapshot = WindowSnapshot(
        totalCalls = count,
        successCalls = successCount,
        errorCalls = errorCount,
        slowSuccessCalls = slowSuccessCount,
        slowErrorCalls = slowErrorCount,
    )

    override fun reset() {
        outcomes.fill(null)
        head = 0
        count = 0
        successCount = 0
        errorCount = 0
        slowSuccessCount = 0
        slowErrorCount = 0
    }

    private fun addToTotals(outcome: CallOutcome) {
        when (outcome) {
            CallOutcome.SUCCESS -> successCount++
            CallOutcome.ERROR -> errorCount++
            CallOutcome.SLOW_SUCCESS -> slowSuccessCount++
            CallOutcome.SLOW_ERROR -> slowErrorCount++
        }
    }

    private fun subtractFromTotals(outcome: CallOutcome) {
        when (outcome) {
            CallOutcome.SUCCESS -> successCount--
            CallOutcome.ERROR -> errorCount--
            CallOutcome.SLOW_SUCCESS -> slowSuccessCount--
            CallOutcome.SLOW_ERROR -> slowErrorCount--
        }
    }
}

// ---------------------------------------------------------------------------
// TIME_BASED — entries expire after `windowSeconds` seconds
// ---------------------------------------------------------------------------

internal data class TimedEntry(val timestampNanos: Long, val outcome: CallOutcome)

internal fun monotonicNanosClock(): () -> Long {
    val start = TimeSource.Monotonic.markNow()
    return { start.elapsedNow().inWholeNanoseconds }
}

internal class TimeBasedSlidingWindow(
    private val windowSeconds: Int,
    private val clock: () -> Long = monotonicNanosClock(),
) : SlidingWindowMetrics {
    // Stored in insertion order; we trim from the front
    private val entries = ArrayDeque<TimedEntry>()

    private var successCount = 0
    private var errorCount = 0
    private var slowSuccessCount = 0
    private var slowErrorCount = 0

    override fun record(outcome: CallOutcome, duration: Duration) {
        evictStale()
        entries.addLast(TimedEntry(clock(), outcome))
        addToTotals(outcome)
    }

    override fun snapshot(): WindowSnapshot {
        evictStale()
        return WindowSnapshot(
            totalCalls = entries.size,
            successCalls = successCount,
            errorCalls = errorCount,
            slowSuccessCalls = slowSuccessCount,
            slowErrorCalls = slowErrorCount,
        )
    }

    override fun reset() {
        entries.clear()
        successCount = 0
        errorCount = 0
        slowSuccessCount = 0
        slowErrorCount = 0
    }

    private fun evictStale() {
        val cutoffNanos = clock() - windowSeconds * 1_000_000_000L
        while (entries.isNotEmpty() && entries.first().timestampNanos < cutoffNanos) {
            subtractFromTotals(entries.removeFirst().outcome)
        }
    }

    private fun addToTotals(outcome: CallOutcome) {
        when (outcome) {
            CallOutcome.SUCCESS -> successCount++
            CallOutcome.ERROR -> errorCount++
            CallOutcome.SLOW_SUCCESS -> slowSuccessCount++
            CallOutcome.SLOW_ERROR -> slowErrorCount++
        }
    }

    private fun subtractFromTotals(outcome: CallOutcome) {
        when (outcome) {
            CallOutcome.SUCCESS -> successCount--
            CallOutcome.ERROR -> errorCount--
            CallOutcome.SLOW_SUCCESS -> slowSuccessCount--
            CallOutcome.SLOW_ERROR -> slowErrorCount--
        }
    }
}
