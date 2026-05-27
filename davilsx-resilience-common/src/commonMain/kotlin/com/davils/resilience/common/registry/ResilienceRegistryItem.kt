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

package com.davils.resilience.common.registry

import com.davils.resilience.common.ResilienceComponent

/**
 * Represents an item stored in a [ResilienceRegistry].
 *
 * This data class pairs a unique name with an asynchronous disposable item.
 * It is used for bulk operations and convenient item management within the registry.
 *
 * @param T The type of the item, which must implement [ResilienceComponent].
 * @since 1.0.0
 */
public data class ResilienceRegistryItem<T : ResilienceComponent<*, *>>(
    /**
     * The unique name associated with the registry item.
     *
     * This name is used for lookups and must match the registry's name constraints.
     *
     * @since 1.0.0
     */
    public val name: String,

    /**
     * The actual item instance stored in the registry.
     *
     * The item implements [ResilienceComponent] and will be disposed of when removed from the registry.
     *
     * @since 1.0.0
     */
    public val item: T
)
