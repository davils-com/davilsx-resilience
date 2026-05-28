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

package com.davils.resilience.common.registry.entity

import com.davils.resilience.common.ResilienceComponent

/**
 * Represents an entry in a [com.davils.resilience.common.registry.ResilienceRegistry].
 *
 * This class wraps a resilience component along with its associated tags and metadata.
 * Tags are typically used for categorization and metrics, while metadata can store
 * additional arbitrary information.
 *
 * @param C The type of the resilience component.
 * @since 1.0.0
 */
public data class ResilienceRegistryEntry<C : ResilienceComponent<*, *>>(
    /**
     * The resilience component instance.
     *
     * @since 1.0.0
     */
    override val component: C,

    /**
     * A map of tags associated with the component.
     *
     * Tags are useful for grouping components and for integration with monitoring systems.
     *
     * @since 1.0.0
     */
    override val tags: Map<String, String> = emptyMap(),

    /**
     * A map of metadata associated with the component.
     *
     * Metadata can store additional context or configuration details.
     *
     * @since 1.0.0
     */
    override val metadata: Map<String, String> = emptyMap()
) : ResilienceRegistryDtoBase<C>
