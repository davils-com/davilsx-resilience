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

package com.davils.resilience.retry.predicate

/**
 * Interface that defines a condition for whether an operation should be retried.
 *
 * Implementations of this interface evaluate either a [Throwable] thrown by the
 * executed block, or a value returned by it, to determine whether the outcome is
 * eligible for a retry attempt.
 *
 * Implementations must be stateless and safe to share across threads and coroutines.
 *
 * @since 1.0.0
 */
public interface Predicate {
    /**
     * Determines whether a retry should be attempted based on the given error.
     *
     * Implementations may inspect the throwable type, message or cause chain to
     * decide whether the failure is transient and should be retried.
     *
     * @param throwable The exception that occurred during the operation, or null if no exception was caught.
     * @return true if the operation should be retried, false otherwise.
     * @since 1.0.0
     */
    public fun shouldRetry(throwable: Throwable?): Boolean

    /**
     * Determines whether a retry should be attempted based on the given successful result value.
     *
     * The default implementation never triggers a retry on successful results,
     * which preserves the historical exception-only retry behavior. Result-based
     * predicates such as [com.davils.resilience.retry.predicate.result.ResultPredicate]
     * override this method to inspect the value returned by the executed block.
     *
     * Implementations must treat unrelated value types defensively and return false
     * for any value they do not understand.
     *
     * @param result The value returned by the executed block. May be null if the block returned null.
     * @return true if the operation should be retried based on the returned value, false otherwise.
     * @since 1.0.0
     */
    public fun shouldRetryOnResult(result: Any?): Boolean = false
}
