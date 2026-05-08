package com.davils.resilience.retry.predicate.throwable

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
    val throwables: MutableList<KClass<out Throwable>>
)
