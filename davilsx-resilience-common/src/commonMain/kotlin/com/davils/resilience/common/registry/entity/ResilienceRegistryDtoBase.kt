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
 * Base interface for information about a registry entry.
 *
 * Provides access to the component instance and its associated tags and metadata.
 *
 * @param C The type of the resilience component.
 * @since 1.0.0
 */
public interface ResilienceRegistryDtoBase<C : ResilienceComponent<*, *>> {
    /**
     * The resilience component instance.
     *
     * @since 1.0.0
     */
    public val component: C

    /**
     * A map of tags associated with the component.
     *
     * @since 1.0.0
     */
    public val tags: Map<String, String>

    /**
     * A map of metadata associated with the component.
     *
     * @since 1.0.0
     */
    public val metadata: Map<String, String>
}
