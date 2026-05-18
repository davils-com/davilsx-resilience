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

package com.davils.resilience.retry

/**
 * Exception thrown when a retry operation has exhausted its configured maximum number of attempts
 * while a result-based predicate still requested another retry.
 *
 * This exception is only raised for result-based exhaustion scenarios, that is, when the executed
 * block did not throw, but the configured [com.davils.resilience.retry.predicate.Predicate]
 * indicated through [com.davils.resilience.retry.predicate.Predicate.shouldRetryOnResult] that the
 * returned value is not acceptable. Exception-based exhaustion continues to propagate the original
 * [Throwable] thrown by the block, since that throwable is the most informative signal in that case.
 *
 * The last observed result is exposed through [lastResult] so that callers may still inspect or log
 * the value that ultimately led to the exhaustion. The value is intentionally typed as [Any] to
 * remain agnostic of the executed block's return type.
 *
 * @since 1.0.0
 */
public class MaxRetriesExceededException(
    /**
     * The number of attempts that were performed before the retry was aborted.
     *
     * This value always equals the configured [RetryData.maxAttempts] when the exception is raised
     * by the built-in retry executors.
     *
     * @since 1.0.0
     */
    public val attempts: Int,

    /**
     * The last value returned by the executed block before the retry was aborted.
     *
     * May be null if the block returned null on its last attempt.
     *
     * @since 1.0.0
     */
    public val lastResult: Any?
) : RuntimeException(
    "Retry exhausted after $attempts attempts; last result was rejected by the configured predicate."
)
