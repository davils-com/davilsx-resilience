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

package com.davils.resilience.retry.predicate.result

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl

/**
 * Data class containing the configuration for a result-based retry predicate.
 *
 * This class is typically instantiated via [ResultPredicateBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ResultPredicateData<T> internal constructor(
    /**
     * The user-supplied condition that decides whether a successful result value should trigger a retry.
     *
     * The condition is invoked for every value returned by the executed block.
     * It must be side-effect free and must not throw, since exceptions raised
     * while evaluating the predicate would otherwise be propagated to the caller.
     *
     * @since 1.0.0
     */
    val condition: (T) -> Boolean,

    /**
     * Indicates whether `null` results are forwarded to [condition].
     *
     * When false (the default), a `null` result short-circuits to `false` without
     * invoking [condition], which keeps user code free from null checks.
     * When true, a `null` value is passed to [condition] as-is, which requires
     * the caller to handle nullability explicitly.
     *
     * @since 1.0.0
     */
    val retryOnNull: Boolean
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        // No validation rules currently, but this is a placeholder for future checks.
    }
}
