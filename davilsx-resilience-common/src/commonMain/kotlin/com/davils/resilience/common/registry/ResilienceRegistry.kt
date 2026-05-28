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

import com.davils.kore.pattern.functional.loan.DisposableAsync
import com.davils.kore.pattern.reactive.event.EventMarker
import com.davils.resilience.common.ResilienceComponent
import com.davils.resilience.common.ResilienceComponentBuilder
import com.davils.resilience.common.ResilienceComponentData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An abstract base class for managing a registry of asynchronous disposable items.
 *
 * This registry provides thread-safe access to a collection of items identified by unique names.
 * All items stored in the registry must implement the [DisposableAsync] interface, allowing
 * them to be properly cleaned up when removed or when the registry is cleared.
 *
 * The registry enforces name constraints using a regular expression.
 *
 * @since 1.0.0
 */
public abstract class ResilienceRegistry<
        E : EventMarker,
        D : ResilienceComponentData,
        B : ResilienceComponentBuilder<D>,
        C : ResilienceComponent<D, E>
> : DisposableAsync {
    protected abstract val componentBuilder: B
    public abstract fun default(builder: B.() -> Unit)
    public abstract fun create(builder: (B.() -> Unit)? = null): C
    private val mutex = Mutex()
    private val registry = mutableMapOf<String, C>()

    private suspend inline fun <R> withLock(
        block: (MutableMap<String, C>) -> R
    ): R = mutex.withLock {
        block(registry)
    }

    private fun putUnsafe(map: MutableMap<String, C>, name: String, item: C): Boolean {
        if (map.containsKey(name)) return false
        map[name] = item
        return true
    }

    private fun removeUnsafe(map: MutableMap<String, C>, name: String): C? {
        return map.remove(name)
    }

    private fun lookupUnsafe(map: MutableMap<String, C>, name: String): C? {
        return map[name]
    }

    private fun existsUnsafe(map: MutableMap<String, C>, name: String): Boolean {
        return map.containsKey(name)
    }

    private fun validateName(name: String) {
        require(name.matches(NAME_REGEX)) { "Registry item name must match regex: $NAME_REGEX" }
    }

    override suspend fun dispose() {
        clear()
    }

    /**
     * Adds an item to the registry with the specified name.
     *
     * The name must match the required naming convention. If an item with the same name
     * already exists, the operation will fail and return false.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name does not match the required naming convention.
     * @since 1.0.0
     */
    public suspend fun put(name: String, item: C): Boolean {
        validateName(name)
        return withLock { map ->
            putUnsafe(map, name, item)
        }
    }

    /**
     * Adds a [ResilienceRegistryItem] to the registry.
     *
     * This is a convenience method that uses the name and item from the provided [ResilienceRegistryItem].
     *
     * @param item The [ResilienceRegistryItem] containing the name and the item instance.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name does not match the required naming convention.
     * @since 1.0.0
     */
    public suspend fun put(item: ResilienceRegistryItem<C>): Boolean = put(item.name, item.item)

    /**
     * Adds an item to the registry if the specified condition is met.
     *
     * The name must match the required naming convention. If the condition returns false
     * or if an item with the same name already exists, the item will not be added.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @param condition A lambda that determines whether the item should be added based on its name and value.
     * @return True if the item was successfully added, false otherwise.
     * @throws IllegalArgumentException If the name does not match the required naming convention.
     * @since 1.0.0
     */
    public suspend fun putIf(name: String, item: C, condition: (name: String, item: C) -> Boolean): Boolean {
        validateName(name)
        return withLock { map ->
            if (!condition(name, item)) return@withLock false
            putUnsafe(map, name, item)
        }
    }

    /**
     * Adds multiple items to the registry from a map.
     *
     * All names must match the required naming convention. If any of the items already exist
     * in the registry, an [IllegalArgumentException] is thrown and none of the items are added.
     *
     * @param items A map where keys are unique names and values are the items to store.
     * @throws IllegalArgumentException If any name is invalid or if any item already exists in the registry.
     * @since 1.0.0
     */
    public suspend fun putAll(items: Map<String, C>) {
        items.keys.forEach { validateName(it) }

        withLock { map ->
            if (items.keys.any { it in map }) {
                throw IllegalArgumentException("Some items already exist in the registry")
            }
            map.putAll(items)
        }
    }

    /**
     * Adds multiple items to the registry from an iterable of [ResilienceRegistryItem].
     *
     * @param items An iterable containing [ResilienceRegistryItem] instances to be added.
     * @throws IllegalArgumentException If any name is invalid or if any item already exists in the registry.
     * @since 1.0.0
     */
    public suspend fun putAll(items: Iterable<ResilienceRegistryItem<C>>) {
        putAll(items.associate { it.name to it.item })
    }

    /**
     * Adds multiple items to the registry provided as varargs.
     *
     * @param items Variable number of [ResilienceRegistryItem] instances to be added.
     * @throws IllegalArgumentException If any name is invalid or if any item already exists in the registry.
     * @since 1.0.0
     */
    public suspend fun putAll(vararg items: ResilienceRegistryItem<C>) {
        putAll(items.asIterable())
    }

    /**
     * Retrieves an item from the registry by its name, or null if no such item exists.
     *
     * @param name The name of the item to look up.
     * @return The item associated with the name, or null if it was not found.
     * @since 1.0.0
     */
    public suspend fun lookupOrNull(name: String): C? {
        return withLock { map ->
            lookupUnsafe(map, name)
        }
    }

    /**
     * Retrieves an item from the registry by its name.
     *
     * @param name The name of the item to look up.
     * @return The item associated with the name.
     * @throws IllegalArgumentException If no item with the given name is found.
     * @since 1.0.0
     */
    public suspend fun lookup(name: String): C {
        return lookupOrNull(name) ?: throw IllegalArgumentException("No item with name $name found in the registry")
    }

    /**
     * Checks if an item with the specified name exists in the registry.
     *
     * @param name The name of the item to check.
     * @return True if an item with the given name exists, false otherwise.
     * @since 1.0.0
     */
    public suspend fun exists(name: String): Boolean {
        return withLock { map ->
            existsUnsafe(map, name)
        }
    }

    /**
     * Removes an item from the registry and disposes of it.
     *
     * If the item exists, it is removed from the internal storage and its [DisposableAsync.dispose]
     * method is called.
     *
     * @param name The name of the item to remove.
     * @since 1.0.0
     */
    public suspend fun remove(name: String) {
        val removed = withLock { map ->
            removeUnsafe(map, name)
        }
        removed?.dispose()
    }

    /**
     * Removes multiple items from the registry by their names and disposes of them.
     *
     * @param names An iterable of names of the items to be removed.
     * @since 1.0.0
     */
    public suspend fun removeAll(names: Iterable<String>) {
        val removedItems = withLock { map ->
            names.mapNotNull { removeUnsafe(map, it) }
        }
        removedItems.forEach { it.dispose() }
    }

    /**
     * Removes multiple items from the registry provided as varargs and disposes of them.
     *
     * @param names Variable number of names of the items to be removed.
     * @since 1.0.0
     */
    public suspend fun removeAll(vararg names: String) {
        removeAll(names.asIterable())
    }

    /**
     * Removes all items from the registry and disposes of each one.
     *
     * After this call, the registry will be empty.
     *
     * @since 1.0.0
     */
    public suspend fun clear() {
        val removedItems = withLock { map ->
            val items = map.values.toList()
            map.clear()
            items
        }

        removedItems.forEach { it.dispose() }
    }

    /**
     * Removes an item from the registry if the specified condition is met and disposes of it.
     *
     * @param name The name of the item to check and potentially remove.
     * @param condition A lambda that determines whether the item should be removed.
     * @return True if the item was found and the condition was met (and thus removed), false otherwise.
     * @since 1.0.0
     */
    public suspend fun removeIf(
        name: String,
        condition: (C) -> Boolean
    ): Boolean {
        val removed = withLock { map ->
            val item = lookupUnsafe(map, name) ?: return@withLock null
            if (!condition(item)) return@withLock null
            removeUnsafe(map, name)
        }

        removed?.dispose()
        return removed != null
    }

    public suspend fun getOrCreate(name: String, builder: (B.() -> Unit )? = null): C {
        validateName(name)

        return withLock { map ->
            val item = lookupUnsafe(map, name)
            if (item != null) return@withLock item

            val newItem = create(builder)
            putUnsafe(map, name, newItem)
            return@withLock newItem
        }
    }

    public suspend fun getOrCreateIf(name: String, condition: (C) -> Boolean, builder: (B.() -> Unit)? = null): C {
        validateName(name)

        return withLock { map ->
            val item = lookupUnsafe(map, name)
            if (item != null) {
                require(condition(item)) { "Existing registry item '$name' does not satisfy the condition" }
                return@withLock item
            }

            val newItem = create(builder)
            putUnsafe(map, name, newItem)
            return@withLock newItem
        }
    }


    /**
     * Operator for retrieving an item from the registry.
     *
     * @param name The name of the item to look up.
     * @return The item associated with the name.
     * @throws IllegalArgumentException If no item with the given name is found.
     * @since 1.0.0
     */
    public suspend operator fun get(name: String): C {
        return lookupOrNull(name) ?: throw IllegalArgumentException("No item with name $name found in the registry")
    }

    /**
     * Operator for adding a [ResilienceRegistryItem] to the registry.
     *
     * @param item The [ResilienceRegistryItem] to be added.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend operator fun plusAssign(item: ResilienceRegistryItem<C>) {
        put(item)
    }

    /**
     * Operator for removing an item from the registry by its name.
     *
     * @param name The name of the item to be removed.
     * @since 1.0.0
     */
    public suspend operator fun minusAssign(name: String) {
        remove(name)
    }

    /**
     * Unary minus operator for removing an item from the registry using its name as the receiver.
     *
     * Example usage: `-"my-item"` within the context of the registry.
     *
     * @since 1.0.0
     */
    public suspend operator fun String.unaryMinus() {
        remove(this)
    }

    private companion object {
        val NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._:-]*$")
    }
}
