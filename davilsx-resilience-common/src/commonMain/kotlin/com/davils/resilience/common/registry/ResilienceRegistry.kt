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
import kotlinx.atomicfu.atomic
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
    private val defaultData = atomic<D?>(null)
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

    private suspend fun disposeAll(items: Iterable<C>) {
        var firstException: Throwable? = null
        items.forEach {
            try {
                it.dispose()
            } catch (t: Throwable) {
                if (firstException == null) firstException = t
            }
        }
        firstException?.let { throw it }
    }

    override suspend fun dispose() {
        clear()
    }

    /**
     * Creates a new instance of the component builder.
     *
     * @return A new builder instance.
     * @since 1.0.0
     */
    protected abstract fun createBuilder(): B

    /**
     * Creates a new instance of the component using the provided data.
     *
     * @param data The configuration data for the component.
     * @return A new component instance.
     * @since 1.0.0
     */
    protected abstract fun createComponent(data: D): C

    /**
     * Returns the current default configuration data.
     *
     * If no default data has been set, it is initialized using a new builder.
     *
     * @return The current default configuration data.
     * @since 1.0.0
     */
    protected fun getDefaultData(): D {
        val current = defaultData.value
        if (current != null) return current

        val initial = createBuilder().produce()
        defaultData.compareAndSet(null, initial)
        return defaultData.value!!
    }

    /**
     * Configures the default settings for the registry.
     *
     * This method creates a new builder, applies the provided [builder] block,
     * and stores the resulting data as the new default configuration.
     * Subsequent calls to [create] without a builder block will use these defaults.
     *
     * @param builder A lambda block to configure the default settings.
     * @since 1.0.0
     */
    public fun default(builder: B.() -> Unit) {
        val b = createBuilder()
        b.apply(builder)
        defaultData.value = b.produce()
    }

    /**
     * Creates a new component instance using the provided builder block or defaults.
     *
     * @param builder An optional configuration builder block.
     * @return A new component instance.
     * @since 1.0.0
     */
    public fun create(builder: (B.() -> Unit)? = null): C {
        return if (builder == null) {
            createComponent(getDefaultData())
        } else {
            val b = createBuilder()
            b.apply(builder)
            createComponent(b.produce())
        }
    }

    /**
     * Adds an item to the registry with the specified name.
     *
     * The name must match the required naming convention.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun put(name: String, item: C) {
        if (!tryPut(name, item)) {
            throw IllegalArgumentException("Item with name '$name' already exists in the registry")
        }
    }

    /**
     * Attempts to add an item to the registry with the specified name.
     *
     * The name must match the required naming convention. If an item with the same name
     * already exists, the operation will return false without adding the item.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name does not match the required naming convention.
     * @since 1.0.0
     */
    public suspend fun tryPut(name: String, item: C): Boolean {
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
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun put(item: ResilienceRegistryItem<C>) {
        put(item.name, item.item)
    }

    /**
     * Attempts to add a [ResilienceRegistryItem] to the registry.
     *
     * @param item The [ResilienceRegistryItem] containing the name and the item instance.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPut(item: ResilienceRegistryItem<C>): Boolean = tryPut(item.name, item.item)

    /**
     * Adds an item to the registry if the specified condition is met.
     *
     * The name must match the required naming convention. If the condition returns false,
     * the item will not be added and the method returns false.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @param condition A lambda that determines whether the item should be added based on its name and value.
     * @return True if the item was successfully added, false otherwise.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun putIf(name: String, item: C, condition: (name: String, item: C) -> Boolean): Boolean {
        validateName(name)
        return withLock { map ->
            if (existsUnsafe(map, name)) {
                throw IllegalArgumentException("Item with name '$name' already exists in the registry")
            }
            if (!condition(name, item)) return@withLock false
            putUnsafe(map, name, item)
        }
    }

    /**
     * Attempts to add an item to the registry if the specified condition is met.
     *
     * If the item already exists, this method returns false.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @param condition A lambda that determines whether the item should be added based on its name and value.
     * @return True if the item was successfully added, false otherwise.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPutIf(name: String, item: C, condition: (name: String, item: C) -> Boolean): Boolean {
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
        if (!tryPutAll(items)) {
            throw IllegalArgumentException("Some items already exist in the registry")
        }
    }

    /**
     * Attempts to add multiple items to the registry from a map.
     *
     * All names must match the required naming convention. If any of the items already exist
     * in the registry, the operation returns false and none of the items are added.
     *
     * @param items A map where keys are unique names and values are the items to store.
     * @return True if all items were successfully added, false if any item already exists.
     * @throws IllegalArgumentException If any name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPutAll(items: Map<String, C>): Boolean {
        items.keys.forEach { validateName(it) }

        return withLock { map ->
            if (items.keys.any { it in map }) {
                return@withLock false
            }
            map.putAll(items)
            true
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
     * Attempts to add multiple items to the registry from an iterable of [ResilienceRegistryItem].
     *
     * @param items An iterable containing [ResilienceRegistryItem] instances to be added.
     * @return True if all items were successfully added, false if any item already exists.
     * @throws IllegalArgumentException If any name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPutAll(items: Iterable<ResilienceRegistryItem<C>>): Boolean {
        return tryPutAll(items.associate { it.name to it.item })
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
     * Attempts to add multiple items to the registry provided as varargs.
     *
     * @param items Variable number of [ResilienceRegistryItem] instances to be added.
     * @return True if all items were successfully added, false if any item already exists.
     * @throws IllegalArgumentException If any name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPutAll(vararg items: ResilienceRegistryItem<C>): Boolean {
        return tryPutAll(items.asIterable())
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
        disposeAll(removedItems)
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

        disposeAll(removedItems)
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

    /**
     * Returns a set of all names registered in the registry.
     *
     * @return A set containing all item names.
     * @since 1.0.0
     */
    public suspend fun names(): Set<String> = withLock { it.keys.toSet() }

    /**
     * Returns a list of all items registered in the registry.
     *
     * @return A list containing all items.
     * @since 1.0.0
     */
    public suspend fun values(): List<C> = withLock { it.values.toList() }

    /**
     * Returns the number of items in the registry.
     *
     * @return The count of registered items.
     * @since 1.0.0
     */
    public suspend fun size(): Int = withLock { it.size }

    /**
     * Checks if the registry is empty.
     *
     * @return True if there are no items, false otherwise.
     * @since 1.0.0
     */
    public suspend fun isEmpty(): Boolean = withLock { it.isEmpty() }

    /**
     * Retrieves an item from the registry by its name, or null if no such item exists.
     *
     * Alias for [lookupOrNull].
     *
     * @param name The name of the item to look up.
     * @return The item associated with the name, or null if it was not found.
     * @since 1.0.0
     */
    public suspend fun getOrNull(name: String): C? = lookupOrNull(name)

    /**
     * Retrieves an item from the registry by its name, or creates it if it doesn't exist.
     *
     * @param name The unique name of the item.
     * @param builder An optional configuration builder block for creating a new item.
     * @return The existing or newly created item.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
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

    /**
     * Retrieves an item from the registry by its name, or creates it if it doesn't exist,
     * provided the existing item satisfies the given condition.
     *
     * @param name The unique name of the item.
     * @param condition A lambda that checks the existing item.
     * @param builder An optional configuration builder block for creating a new item.
     * @return The existing or newly created item.
     * @throws IllegalArgumentException If the existing item does not satisfy the condition.
     * @since 1.0.0
     */
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
     * Creates and registers a new item in the registry using the provided builder.
     *
     * @param name The unique name to associate with the item.
     * @param builder An optional configuration builder block.
     * @return The newly created item.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun create(name: String, builder: (B.() -> Unit)? = null): C {
        validateName(name)
        return withLock { map ->
            if (existsUnsafe(map, name)) {
                throw IllegalArgumentException("Item with name '$name' already exists in the registry")
            }
            val newItem = create(builder)
            putUnsafe(map, name, newItem)
            newItem
        }
    }

    /**
     * Adds an item to the registry, replacing any existing item with the same name.
     *
     * If an item already exists, it is disposed of before being replaced.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun replace(name: String, item: C) {
        validateName(name)
        val oldItem = withLock { map ->
            val existing = map[name]
            map[name] = item
            existing
        }
        oldItem?.dispose()
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
