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

package com.davils.resilience.circuitbreaker.predicate

import com.davils.kore.annotation.KoreDsl
import kotlin.reflect.KClass

/**
 * Determines whether a thrown exception should be recorded as a failure or ignored
 * by the [com.davils.resilience.circuitbreaker.CircuitBreaker].
 *
 * The ignore check always wins over the record check — if an exception matches both
 * predicates it is ignored, not recorded as a failure.
 *
 * @since 1.0.0
 */
public fun interface ExceptionPredicate {
    /**
     * Returns `true` if [throwable] should be evaluated by this predicate (recorded or ignored).
     *
     * @since 1.0.0
     */
    public fun test(throwable: Throwable): Boolean
}

/** Default predicate that matches all exceptions. */
internal val recordAllExceptions: ExceptionPredicate = ExceptionPredicate { true }

/** Default predicate that ignores no exceptions. */
internal val ignoreNoExceptions: ExceptionPredicate = ExceptionPredicate { false }

/**
 * Builder for constructing an [ExceptionPredicate] from a list of [KClass] types.
 *
 * @since 1.0.0
 */
@KoreDsl
public class ExceptionPredicateBuilder internal constructor() {
    private val types = mutableListOf<KClass<out Throwable>>()
    private var includeCauseChain: Boolean = false

    /** Include all subclasses of [T] as matches. */
    public fun <T : Throwable> on(type: KClass<T>) {
        types.add(type)
    }

    /** Include all subclasses of [T] as matches (reified). */
    public inline fun <reified T : Throwable> on() {
        on(T::class)
    }

    /** When true, causes within the exception chain are also checked. */
    public fun includeCauseChain(value: Boolean = true) {
        includeCauseChain = value
    }

    internal fun build(): ExceptionPredicate {
        val snapshot = types.toList()
        val checkCause = includeCauseChain
        if (snapshot.isEmpty()) return recordAllExceptions
        return ExceptionPredicate { throwable ->
            matchesAny(throwable, snapshot, checkCause)
        }
    }
}

private fun matchesAny(
    throwable: Throwable,
    types: List<KClass<out Throwable>>,
    checkCause: Boolean
): Boolean {
    if (!checkCause) return types.any { it.isInstance(throwable) }
    var current: Throwable? = throwable
    val visited = mutableSetOf<Throwable>()
    while (current != null && visited.add(current)) {
        if (types.any { it.isInstance(current) }) return true
        current = current.cause
    }
    return false
}
