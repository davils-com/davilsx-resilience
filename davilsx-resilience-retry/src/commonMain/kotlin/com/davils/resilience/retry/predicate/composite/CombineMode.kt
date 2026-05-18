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
