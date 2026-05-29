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

package com.davils.resilience.metrics.retry.data

/**
 * Data class representing the metrics related to the overall operation of a retry component.
 *
 * These metrics measure the final outcome for the caller after all retry attempts
 * have been completed or aborted.
 *
 * @since 1.0.0
 */
public data class RetryCallMetrics(
    /**
     * The total number of calls protected by the retry component.
     *
     * This count is incremented at the start of the first attempt of any call.
     *
     * @since 1.0.0
     */
    public val totalCalls: Long,

    /**
     * The number of operations that eventually succeeded.
     *
     * This includes operations that succeeded on the first attempt as well as those
     * that succeeded after one or more retries.
     *
     * @since 1.0.0
     */
    public val successfulCalls: Long,

    /**
     * The number of operations that failed permanently because the maximum number of attempts was reached.
     *
     * This count represents calls where all allowed retries were exhausted without success.
     *
     * @since 1.0.0
     */
    public val exhaustedCalls: Long,

    /**
     * The number of operations that failed and were not retried because the error was non-retryable.
     *
     * This occurs when the configured predicate determines that an exception or result
     * should not trigger a retry, even if the maximum attempt count has not been reached.
     *
     * @since 1.0.0
     */
    public val failedNonRetryableCalls: Long,

    /**
     * The number of operations that were canceled during execution.
     *
     * Cancellation typically occurs due to coroutine cancellation or when the
     * retry instance is disposed.
     *
     * @since 1.0.0
     */
    public val canceledCalls: Long,
)
