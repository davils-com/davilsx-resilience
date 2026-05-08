package com.davils.resilience.retry.predicate.composite

/**
 * Defines how multiple predicates are combined inside a [CompositePredicate].
 *
 * The combine mode controls whether the composite predicate behaves like a
 * logical OR or like a logical AND across its delegates. It is applied
 * uniformly to both the throwable-based and the result-based decision paths.
 *
 * @since 1.0.0
 */
public enum class CombineMode {
    /**
     * Logical OR semantics: the composite triggers a retry as soon as any delegate predicate opts in.
     *
     * This is the typical choice for combining heterogeneous predicates such as
     * a throwable predicate and a result predicate, because each delegate is
     * authoritative within its own domain and unrelated inputs naturally
     * short-circuit to false.
     *
     * @since 1.0.0
     */
    ANY,

    /**
     * Logical AND semantics: the composite triggers a retry only when every delegate predicate opts in.
     *
     * This mode is rarely useful when mixing throwable and result predicates,
     * because each delegate ignores the dimension it is not responsible for.
     * It is intended for combining multiple predicates that operate on the same
     * input dimension and must all agree before a retry is attempted.
     *
     * @since 1.0.0
     */
    ALL
}
