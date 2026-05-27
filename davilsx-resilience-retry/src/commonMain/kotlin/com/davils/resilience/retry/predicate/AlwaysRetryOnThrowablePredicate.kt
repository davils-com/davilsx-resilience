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
 * A predicate that always returns true if a non-null [Throwable] is provided.
 *
 * This implementation is useful when any exception should trigger a retry attempt,
 * regardless of its type or properties.
 *
 * @since 1.0.0
 */
public class AlwaysRetryOnThrowablePredicate internal constructor() : Predicate {
    /**
     * Returns true if the provided throwable is not null.
     *
     * @param throwable The exception to evaluate.
     * @return true if [throwable] is not null, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetryOnThrowable(throwable: Throwable?): Boolean = throwable != null
}

/**
 * Creates an instance of [AlwaysRetryOnThrowablePredicate].
 *
 * @return A [Predicate] that retries on any [Throwable].
 * @since 1.0.0
 */
public fun alwaysRetryOnThrowablePredicate(): AlwaysRetryOnThrowablePredicate = AlwaysRetryOnThrowablePredicate()
