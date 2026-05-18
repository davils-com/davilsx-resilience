package com.davils.resilience.retry.predicate.result

import com.davils.resilience.retry.predicate.Predicate

/**
 * A predicate that triggers retries based on the value returned by an executed block.
 *
 * In contrast to throwable-based predicates, this implementation evaluates the
 * successful result of an operation against a user-defined condition. This is
 * particularly useful for APIs whose failures are reported as values rather
 * than thrown exceptions, such as HTTP clients that return response objects
 * with status codes, or polling endpoints returning a "not ready" sentinel.
 *
 * The exception-based [shouldRetry] always returns false, which keeps result
 * predicates orthogonal to throwable predicates. To combine both behaviors,
 * configure two retries or wrap multiple predicates explicitly.
 *
 * @since 1.0.0
 */
public class ResultPredicate<T> internal constructor(private val data: ResultPredicateData<T>) : Predicate {
    /**
     * Always returns false because this predicate does not react to thrown exceptions.
     *
     * Throwable-based retries should be expressed with a dedicated throwable predicate.
     *
     * @param throwable The exception to evaluate. Ignored by this predicate.
     * @return Always false.
     * @since 1.0.0
     */
    override fun shouldRetry(throwable: Throwable?): Boolean = false

    /**
     * Evaluates the configured condition against the successful result value.
     *
     * If the value is `null` and `retryOnNull` is disabled, no retry is triggered
     * and the configured condition is not invoked. Otherwise the value is forwarded
     * to the condition. Note that due to JVM type erasure the runtime cannot enforce
     * that [result] is actually an instance of [T]; callers are expected to use
     * this predicate with a retry whose block returns [T].
     *
     * @param result The value returned by the executed block.
     * @return true if the configured condition returns true for [result], false otherwise.
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun shouldRetryOnResult(result: Any?): Boolean {
        if (result == null) {
            return if (data.retryOnNull) data.condition(null as T) else false
        }
        val typed = result as? T ?: return false
        return data.condition(typed)
    }
}

/**
 * Creates a [ResultPredicate] using a DSL builder.
 *
 * This function allows for easy configuration of a result-based retry predicate.
 *
 * Example:
 * ```kotlin
 * val predicate = resultPredicate<HttpResponse> {
 *     retryIf { it.status.value in 500..599 }
 * }
 * ```
 *
 * @param T The type of the value produced by the executed block.
 * @param builder A lambda to configure the [ResultPredicateBuilder].
 * @return A [ResultPredicate] configured with the specified condition.
 * @since 1.0.0
 */
public fun <T> resultPredicate(builder: ResultPredicateBuilder<T>.() -> Unit): ResultPredicate<T> {
    val resultBuilder = ResultPredicateBuilder<T>()
    resultBuilder.builder()
    val data = resultBuilder.produce()
    return ResultPredicate(data)
}
