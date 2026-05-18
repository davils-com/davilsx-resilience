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

package com.davils.resilience.retry.predicate.composite

import com.davils.resilience.retry.predicate.Predicate

/**
 * A predicate that combines multiple [Predicate] instances under a shared [CombineMode].
 *
 * The composite delegates to each configured predicate for both the
 * throwable-based and the result-based decision paths. This allows callers
 * to express enterprise-grade retry policies such as "retry on `IOException`
 * or HTTP 5xx" by mixing a [com.davils.resilience.retry.predicate.throwable.ThrowablePredicate]
 * with a [com.davils.resilience.retry.predicate.result.ResultPredicate] inside a single
 * retry instance.
 *
 * The composite preserves the semantics of each delegate: a throwable
 * predicate only opts in on matching exceptions, a result predicate only
 * opts in on matching values, and unrelated dimensions naturally evaluate
 * to false. As a consequence, [CombineMode.ANY] is the typical choice when
 * mixing heterogeneous predicate kinds.
 *
 * Instances are immutable and safe to share across threads and coroutines,
 * provided the supplied delegates are themselves stateless.
 *
 * @since 1.0.0
 */
public class CompositePredicate internal constructor(
    private val predicates: List<Predicate>,
    private val mode: CombineMode
) : Predicate {
    /**
     * Evaluates the throwable against every delegate predicate using the configured [CombineMode].
     *
     * For [CombineMode.ANY] the composite returns true as soon as any delegate
     * opts in. For [CombineMode.ALL] every delegate must opt in. An empty
     * delegate list returns false for [CombineMode.ANY] and true for
     * [CombineMode.ALL] following standard Kotlin collection semantics.
     *
     * @param throwable The exception to evaluate, or null if no exception was caught.
     * @return true when the composition of all delegate decisions indicates that a retry should be attempted, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetry(throwable: Throwable?): Boolean = when (mode) {
        CombineMode.ANY -> predicates.any { it.shouldRetry(throwable) }
        CombineMode.ALL -> predicates.all { it.shouldRetry(throwable) }
    }

    /**
     * Evaluates the successful result value against every delegate predicate using the configured [CombineMode].
     *
     * Delegates that do not implement result-based retries fall back to the
     * default [Predicate.shouldRetryOnResult] implementation, which returns
     * false. This keeps throwable-only predicates inert on the result path
     * when combined via [CombineMode.ANY].
     *
     * @param result The value produced by the executed block. May be null.
     * @return true when the composition of all delegate decisions indicates that a retry should be attempted, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetryOnResult(result: Any?): Boolean = when (mode) {
        CombineMode.ANY -> predicates.any { it.shouldRetryOnResult(result) }
        CombineMode.ALL -> predicates.all { it.shouldRetryOnResult(result) }
    }
}

/**
 * Combines the supplied [predicates] using [CombineMode.ANY] semantics.
 *
 * The resulting predicate triggers a retry as soon as any delegate opts in
 * on either the throwable path or the result path. This is the recommended
 * combinator for mixing throwable-based and result-based predicates inside
 * a single retry instance.
 *
 * @param predicates The delegate predicates that participate in the composition. May be empty, in which case the composite never opts in.
 * @return A [Predicate] that returns true whenever at least one delegate predicate returns true for the same input.
 * @since 1.0.0
 */
public fun anyOf(vararg predicates: Predicate): Predicate =
    CompositePredicate(predicates.toList(), CombineMode.ANY)

/**
 * Combines the supplied [predicates] using [CombineMode.ALL] semantics.
 *
 * The resulting predicate triggers a retry only when every delegate opts in
 * on the corresponding decision path. Mixing predicate kinds (throwable and
 * result) under [CombineMode.ALL] is rarely meaningful and should be done
 * deliberately, because each delegate ignores inputs outside its domain.
 *
 * @param predicates The delegate predicates that participate in the composition. An empty list yields a predicate that always opts in, mirroring standard `all { }` semantics.
 * @return A [Predicate] that returns true only when every delegate predicate returns true for the same input.
 * @since 1.0.0
 */
public fun allOf(vararg predicates: Predicate): Predicate =
    CompositePredicate(predicates.toList(), CombineMode.ALL)

/**
 * Returns a [Predicate] that opts in whenever either the receiver or [other] opts in.
 *
 * Convenience infix-style operator for fluently combining two predicates with
 * [CombineMode.ANY]. Equivalent to `anyOf(this, other)`.
 *
 * @param other The predicate to combine with the receiver.
 * @return A composite [Predicate] using [CombineMode.ANY] semantics across the two operands.
 * @since 1.0.0
 */
public fun Predicate.or(other: Predicate): Predicate = anyOf(this, other)

/**
 * Returns a [Predicate] that opts in only when both the receiver and [other] opt in.
 *
 * Convenience infix-style operator for fluently combining two predicates with
 * [CombineMode.ALL]. Equivalent to `allOf(this, other)`.
 *
 * @param other The predicate to combine with the receiver.
 * @return A composite [Predicate] using [CombineMode.ALL] semantics across the two operands.
 * @since 1.0.0
 */
public fun Predicate.and(other: Predicate): Predicate = allOf(this, other)
