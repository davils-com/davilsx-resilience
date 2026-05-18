package com.davils.resilience.retry.predicate.throwable

import com.davils.resilience.retry.predicate.Predicate
import kotlin.reflect.KClass

/**
 * A predicate that retries based on a specific set of [Throwable] classes.
 *
 * This implementation checks if the occurred exception is an instance of any
 * of the configured throwable types. When [ThrowablePredicateData.includeCauseChain]
 * is enabled, the exception's cause chain is also inspected, which is useful for
 * exceptions that wrap an underlying transient failure.
 *
 * Matching against the ignore list always wins over the retry list, even when
 * cause-chain inspection is enabled.
 *
 * @since 1.0.0
 */
public class ThrowablePredicate internal constructor(private val data: ThrowablePredicateData) : Predicate {
    /**
     * Determines whether to retry based on whether the throwable matches any configured type.
     *
     * If cause-chain inspection is enabled, every exception reachable via
     * [Throwable.cause] is taken into account in addition to the throwable itself.
     *
     * @param throwable The exception to evaluate.
     * @return true if the throwable (or any of its causes when cause-chain inspection is enabled)
     * matches any of the configured retry types and none of the ignore types, false otherwise.
     * @since 1.0.0
     */
    override fun shouldRetry(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        if (matchesAny(throwable, data.ignoreThrowables)) return false
        if (data.retryOnAll) return true
        return matchesAny(throwable, data.throwables)
    }

    private fun matchesAny(throwable: Throwable, types: List<KClass<out Throwable>>): Boolean {
        if (types.isEmpty()) return false
        if (!data.includeCauseChain) {
            return types.any { it.isInstance(throwable) }
        }
        var current: Throwable? = throwable
        val visited = mutableSetOf<Throwable>()
        while (current != null && visited.add(current)) {
            if (types.any { it.isInstance(current) }) return true
            current = current.cause
        }
        return false
    }
}

/**
 * Creates a [ThrowablePredicate] using a DSL builder.
 *
 * This function allows for easy configuration of which exceptions should trigger a retry.
 *
 * @param builder A lambda to configure the [ThrowablePredicateBuilder].
 * @return A [ThrowablePredicate] configured with the specified throwable types.
 * @since 1.0.0
 */
public fun throwablePredicate(builder: ThrowablePredicateBuilder.() -> Unit): ThrowablePredicate {
    val throwableBuilder = ThrowablePredicateBuilder()
    throwableBuilder.builder()
    val data = throwableBuilder.produce()
    return ThrowablePredicate(data)
}
