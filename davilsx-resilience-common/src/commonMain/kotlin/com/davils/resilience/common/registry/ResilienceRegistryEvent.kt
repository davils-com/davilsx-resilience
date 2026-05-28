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

import com.davils.kore.pattern.reactive.event.EventMarker
import com.davils.resilience.common.ResilienceComponent
import com.davils.resilience.common.registry.entity.ResilienceRegistryEntry

/**
 * Base class for all events emitted by a [ResilienceRegistry].
 *
 * These events provide notifications about lifecycle changes and modifications
 * within the registry, such as adding or removing components.
 *
 * @since 1.0.0
 */
public sealed class ResilienceRegistryEvent : EventMarker() {

    /**
     * Event emitted when a new entry is added to the registry.
     *
     * @param name The name of the added entry.
     * @param entry The registry entry that was added.
     * @since 1.0.0
     */
    public data class EntryAdded<C : ResilienceComponent<*, *>>(
        public val name: String,
        public val entry: ResilienceRegistryEntry<C>
    ) : ResilienceRegistryEvent()

    /**
     * Event emitted when an entry is removed from the registry.
     *
     * @param name The name of the removed entry.
     * @param entry The registry entry that was removed.
     * @since 1.0.0
     */
    public data class EntryRemoved<C : ResilienceComponent<*, *>>(
        public val name: String,
        public val entry: ResilienceRegistryEntry<C>
    ) : ResilienceRegistryEvent()

    /**
     * Event emitted when an entry is replaced in the registry.
     *
     * @param name The name of the replaced entry.
     * @param oldEntry The registry entry that was removed.
     * @param newEntry The registry entry that was added.
     * @since 1.0.0
     */
    public data class EntryReplaced<C : ResilienceComponent<*, *>>(
        public val name: String,
        public val oldEntry: ResilienceRegistryEntry<C>,
        public val newEntry: ResilienceRegistryEntry<C>
    ) : ResilienceRegistryEvent()

    /**
     * Event emitted when the registry is cleared.
     *
     * @since 1.0.0
     */
    public data object RegistryCleared : ResilienceRegistryEvent()

    /**
     * Event emitted when the registry is disposed.
     *
     * @since 1.0.0
     */
    public data object RegistryDisposed : ResilienceRegistryEvent()
}
