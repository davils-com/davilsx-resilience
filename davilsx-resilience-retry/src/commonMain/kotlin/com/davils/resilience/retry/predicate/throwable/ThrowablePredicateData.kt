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

package com.davils.resilience.retry.predicate.throwable

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import kotlin.reflect.KClass

/**
 * Data class containing the configuration for a throwable-based retry predicate.
 *
 * This class is typically instantiated via [ThrowablePredicateBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ThrowablePredicateData internal constructor(
    /**
     * The list of exception types that should trigger a retry attempt.
     *
     * @since 1.0.0
     */
    val throwables: List<KClass<out Throwable>>,

    /**
     * The list of exception types that should be ignored and not trigger a retry.
     *
     * @since 1.0.0
     */
    val ignoreThrowables: List<KClass<out Throwable>> = mutableListOf(),

    /**
     * Indicates whether all exceptions should be retried.
     *
     * @since 1.0.0
     */
    val retryOnAll: Boolean = false,

    /**
     * Indicates whether the cause chain of an exception should also be inspected
     * when matching against [throwables] and [ignoreThrowables].
     *
     * When true, the predicate walks the chain produced by [Throwable.cause] and
     * matches if any wrapped exception matches the configured types. This is
     * useful when an underlying transient failure is wrapped by an unrelated
     * runtime exception (e.g. an `ExecutionException` wrapping an `IOException`).
     *
     * Defaults to false to preserve the historical strict matching behavior.
     *
     * @since 1.0.0
     */
    val includeCauseChain: Boolean = false
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (throwables.isEmpty() && !retryOnAll) {
            fail("At least one throwable type must be specified", "throwables")
        }

        if (retryOnAll && throwables.isNotEmpty()) {
            fail("retryOnAll is true, but multiple throwable types are specified", "throwables")
        }
    }
}
