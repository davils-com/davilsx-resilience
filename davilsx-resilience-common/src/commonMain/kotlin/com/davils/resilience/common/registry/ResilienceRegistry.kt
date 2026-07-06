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
import com.davils.kore.pattern.reactive.event.EventBus
import com.davils.kore.pattern.reactive.event.EventMarker
import com.davils.kore.pattern.reactive.event.EventTopic
import com.davils.kore.pattern.reactive.event.eventBus
import com.davils.resilience.common.ResilienceComponent
import com.davils.resilience.common.ResilienceComponentBuilder
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.registry.entity.ResilienceRegistryEntry
import com.davils.resilience.common.registry.entity.ResilienceRegistryItem
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * An abstract base class for managing a registry of asynchronous disposable resilience components.
 *
 * This registry provides thread-safe access to a collection of items identified by unique names.
 * All items stored in the registry must implement the [DisposableAsync] interface, allowing
 * them to be properly cleaned up when removed or when the registry is cleared.
 *
 * The registry enforces name constraints using a regular expression.
 *
 * @param E The type of event marker used by the components.
 * @param D The type of configuration data for the components.
 * @param B The type of builder used to create components.
 * @param C The type of resilience components managed by this registry.
 * @since 1.0.0
 */
public abstract class ResilienceRegistry<
        E : EventMarker,
        D : ResilienceComponentData,
        B : ResilienceComponentBuilder<D>,
        C : ResilienceComponent<D, E>
> : DisposableAsync {
    /**
     * The configuration data for the registry itself.
     *
     * @since 1.0.0
     */
    protected abstract val registryData: ResilienceRegistryData

    /**
     * Creates a new builder instance for the components managed by this registry.
     *
     * @return A new builder instance of type [B].
     * @since 1.0.0
     */
    protected abstract fun createBuilder(): B

    /**
     * Creates a new component instance using the provided configuration data.
     *
     * @param data The configuration data for the new component.
     * @return A new component instance of type [C].
     * @since 1.0.0
     */
    protected abstract fun createComponent(data: D): C

    private val defaultData = atomic<D?>(null)
    private val mutex = Mutex()
    private val registry = mutableMapOf<String, ResilienceRegistryEntry<C>>()
    private val bus: EventBus by lazy {
        eventBus(registryData.eventData.scope) {
            replay = registryData.eventData.replay
            onError = registryData.eventData.onError
            overflowStrategy = registryData.eventData.overflowStrategy
            extraBufferCapacity = registryData.eventData.extraBufferCapacity
            topic<ResilienceRegistryEvent>(EVENT_TOPIC_NAME)
        }
    }
    private val eventBus: EventTopic<ResilienceRegistryEvent> by lazy { bus.topic(EVENT_TOPIC_NAME) }

    private suspend inline fun <R> withLock(
        block: (MutableMap<String, ResilienceRegistryEntry<C>>) -> R
    ): R = mutex.withLock {
        block(registry)
    }

    private fun putUnsafe(
        map: MutableMap<String, ResilienceRegistryEntry<C>>,
        name: String,
        entry: ResilienceRegistryEntry<C>
    ): Boolean {
        if (map.containsKey(name)) return false
        map[name] = entry
        return true
    }

    private fun removeUnsafe(map: MutableMap<String, ResilienceRegistryEntry<C>>, name: String): ResilienceRegistryEntry<C>? {
        return map.remove(name)
    }

    private fun lookupUnsafe(map: MutableMap<String, ResilienceRegistryEntry<C>>, name: String): ResilienceRegistryEntry<C>? {
        return map[name]
    }

    private fun existsUnsafe(map: MutableMap<String, ResilienceRegistryEntry<C>>, name: String): Boolean {
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

    private suspend fun tryPutAllEntries(
        entries: Map<String, ResilienceRegistryEntry<C>>
    ): Boolean {
        if (entries.isEmpty()) return true

        val added = withLock { map ->
            for (key in entries.keys) {
                if (map.containsKey(key)) return@withLock false
            }

            map.putAll(entries)
            true
        }

        if (added) {
            entries.forEach { (name, entry) ->
                eventBus.push(ResilienceRegistryEvent.EntryAdded(name, entry))
            }
        }

        return added
    }

    /**
     * Disposes of the registry and all its registered components.
     *
     * This method clears the registry, disposes of all managed components,
     * and closes the internal event bus.
     *
     * @since 1.0.0
     */
    override suspend fun dispose() {
        eventBus.push(ResilienceRegistryEvent.RegistryDisposed)
        clear()
        bus.dispose()
    }

    /**
     * Subscribes to events of a specific type from the registry's event bus.
     *
     * @param R The type of events to subscribe to.
     * @param eventType The class of the event type.
     * @param onError An optional error handler for the subscription.
     * @param on A lambda to be executed when an event of the specified type is emitted.
     * @return A [Job] representing the subscription.
     * @since 1.0.0
     */
    public fun <R : ResilienceRegistryEvent> subscribe(
        eventType: KClass<R>,
        onError: (suspend (Throwable) -> Unit)? = null,
        on: suspend (R) -> Unit
    ): Job = eventBus.subscribe(onError) { event ->
        eventType.safeCast(event)?.let { on(it) }
    }

    /**
     * Subscribes to events of a specific type from the registry's event bus using reified type parameters.
     *
     * @param R The type of events to subscribe to.
     * @param onError An optional error handler for the subscription.
     * @param on A lambda to be executed when an event of the specified type is emitted.
     * @return A [Job] representing the subscription.
     * @since 1.0.0
     */
    public inline fun <reified R : ResilienceRegistryEvent> subscribe(
        noinline onError: (suspend (Throwable) -> Unit)? = null,
        noinline on: suspend (R) -> Unit
    ): Job = subscribe(R::class, onError, on)


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
     * Adds an item to the registry with the specified name, tags, and metadata.
     *
     * The name must match the required naming convention.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @param tags An optional map of tags to associate with the item.
     * @param metadata An optional map of metadata to associate with the item.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun put(
        name: String,
        item: C,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ) {
        if (!tryPut(name, item, tags, metadata)) {
            throw IllegalArgumentException("Item with name '$name' already exists in the registry")
        }
    }

    /**
     * Attempts to add an item to the registry with the specified name, tags, and metadata.
     *
     * The name must match the required naming convention. If an item with the same name
     * already exists, the operation will return false without adding the item.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store in the registry.
     * @param tags An optional map of tags to associate with the item.
     * @param metadata An optional map of metadata to associate with the item.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name does not match the required naming convention.
     * @since 1.0.0
     */
    public suspend fun tryPut(
        name: String,
        item: C,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        validateName(name)
        val entry = ResilienceRegistryEntry(item, tags, metadata)
        val added = withLock { map ->
            putUnsafe(map, name, entry)
        }
        if (added) {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, entry))
        }
        return added
    }

    /**
     * Adds a [com.davils.resilience.common.registry.entity.ResilienceRegistryItem] to the registry.
     *
     * This is a convenience method that uses the name and item from the provided [com.davils.resilience.common.registry.entity.ResilienceRegistryItem].
     *
     * @param item The [com.davils.resilience.common.registry.entity.ResilienceRegistryItem] containing the name and the item instance.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun put(item: ResilienceRegistryItem<C>) {
        put(item.name, item.component, item.tags, item.metadata)
    }

    /**
     * Attempts to add a [ResilienceRegistryItem] to the registry.
     *
     * @param item The [ResilienceRegistryItem] containing the name, component instance, tags, and metadata.
     * @return True if the item was successfully added, false if an item with the same name already exists.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun tryPut(item: ResilienceRegistryItem<C>): Boolean = tryPut(item.name, item.component, item.tags, item.metadata)

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
        val entry = ResilienceRegistryEntry(item)
        val added = withLock { map ->
            if (existsUnsafe(map, name)) {
                throw IllegalArgumentException("Item with name '$name' already exists in the registry")
            }
            if (!condition(name, item)) return@withLock false
            putUnsafe(map, name, entry)
        }
        if (added) {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, entry))
        }
        return added
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
        val entry = ResilienceRegistryEntry(item)
        val added = withLock { map ->
            if (!condition(name, item)) return@withLock false
            putUnsafe(map, name, entry)
        }
        if (added) {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, entry))
        }
        return added
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
        if (items.isEmpty()) return true

        items.keys.forEach { validateName(it) }

        val entries = items.mapValues { (_, value) ->
            ResilienceRegistryEntry(value)
        }

        return tryPutAllEntries(entries)
    }

    /**
     * Adds multiple items to the registry from an iterable of [ResilienceRegistryItem].
     *
     * @param items An iterable containing [ResilienceRegistryItem] instances to be added.
     * @throws IllegalArgumentException If any name is invalid or if any item already exists in the registry.
     * @since 1.0.0
     */
    public suspend fun putAll(items: Iterable<ResilienceRegistryItem<C>>) {
        if (!tryPutAll(items)) {
            throw IllegalArgumentException("Some items already exist in the registry")
        }
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
        val itemList = items.toList()
        if (itemList.isEmpty()) return true

        itemList.forEach { validateName(it.name) }

        val entries = buildMap {
            for (item in itemList) {
                if (containsKey(item.name)) return false

                put(
                    item.name,
                    ResilienceRegistryEntry(
                        item.component,
                        item.tags,
                        item.metadata
                    )
                )
            }
        }

        return tryPutAllEntries(entries)
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
            lookupUnsafe(map, name)?.component
        }
    }

    /**
     * Retrieves a registry entry by its name, or null if no such entry exists.
     *
     * @param name The name of the entry to look up.
     * @return The entry associated with the name, or null if it was not found.
     * @since 1.0.0
     */
    public suspend fun lookupEntryOrNull(name: String): ResilienceRegistryEntry<C>? {
        return withLock { map ->
            lookupUnsafe(map, name)
        }
    }

    /**
     * Retrieves a registry entry by its name.
     *
     * @param name The name of the entry to look up.
     * @return The entry associated with the name.
     * @throws IllegalArgumentException If no entry with the given name is found.
     * @since 1.0.0
     */
    public suspend fun lookupEntry(name: String): ResilienceRegistryEntry<C> {
        return lookupEntryOrNull(name) ?: throw IllegalArgumentException("No entry with name $name found in the registry")
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
        removed?.let {
            eventBus.push(ResilienceRegistryEvent.EntryRemoved(name, it))
            it.component.dispose()
        }
    }

    /**
     * Removes multiple items from the registry by their names and disposes of them.
     *
     * @param names An iterable of names of the items to be removed.
     * @since 1.0.0
     */
    public suspend fun removeAll(names: Iterable<String>) {
        val removedEntries = withLock { map ->
            names.mapNotNull { name ->
                removeUnsafe(map, name)?.let { name to it }
            }
        }
        removedEntries.forEach { (name, entry) ->
            eventBus.push(ResilienceRegistryEvent.EntryRemoved(name, entry))
        }
        disposeAll(removedEntries.map { it.second.component })
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
        val removedEntries = withLock { map ->
            val entries = map.toList()
            map.clear()
            entries
        }

        removedEntries.forEach { (name, entry) ->
            eventBus.push(ResilienceRegistryEvent.EntryRemoved(name, entry))
        }

        eventBus.push(ResilienceRegistryEvent.RegistryCleared)
        disposeAll(removedEntries.map { it.second.component })
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
            val entry = lookupUnsafe(map, name) ?: return@withLock null
            if (!condition(entry.component)) return@withLock null
            removeUnsafe(map, name)
        }

        removed?.let {
            eventBus.push(ResilienceRegistryEvent.EntryRemoved(name, it))
            it.component.dispose()
        }
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
    public suspend fun values(): List<C> = withLock { it.values.map { entry -> entry.component }.toList() }

    /**
     * Returns a list of all registry entries.
     *
     * @return A list containing all entries.
     * @since 1.0.0
     */
    public suspend fun entries(): List<ResilienceRegistryEntry<C>> = withLock { it.values.toList() }

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
     * @param tags An optional map of tags to associate with the item if it's created.
     * @param metadata An optional map of metadata to associate with the item if it's created.
     * @param builder An optional configuration builder block for creating a new item.
     * @return The existing or newly created item.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun getOrCreate(
        name: String,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap(),
        builder: (B.() -> Unit)? = null
    ): C {
        validateName(name)

        var createdEntry: ResilienceRegistryEntry<C>? = null
        val item = withLock { map ->
            val entry = lookupUnsafe(map, name)
            if (entry != null) return@withLock entry.component

            val newItem = create(builder)
            val newEntry = ResilienceRegistryEntry(newItem, tags, metadata)
            putUnsafe(map, name, newEntry)
            createdEntry = newEntry
            newItem
        }

        createdEntry?.let {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, it))
        }
        return item
    }

    /**
     * Retrieves an item from the registry by its name, or creates it if it doesn't exist,
     * provided the existing item satisfies the given condition.
     *
     * @param name The unique name of the item.
     * @param condition A lambda that checks the existing item.
     * @param tags An optional map of tags to associate with the item if it's created.
     * @param metadata An optional map of metadata to associate with the item if it's created.
     * @param builder An optional configuration builder block for creating a new item.
     * @return The existing or newly created item.
     * @throws IllegalArgumentException If the existing item does not satisfy the condition.
     * @since 1.0.0
     */
    public suspend fun getOrCreateIf(
        name: String,
        condition: (C) -> Boolean,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap(),
        builder: (B.() -> Unit)? = null
    ): C {
        validateName(name)

        var createdEntry: ResilienceRegistryEntry<C>? = null
        val item = withLock { map ->
            val entry = lookupUnsafe(map, name)
            if (entry != null) {
                require(condition(entry.component)) { "Existing registry item '$name' does not satisfy the condition" }
                return@withLock entry.component
            }

            val newItem = create(builder)
            val newEntry = ResilienceRegistryEntry(newItem, tags, metadata)
            putUnsafe(map, name, newEntry)
            createdEntry = newEntry
            newItem
        }

        createdEntry?.let {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, it))
        }
        return item
    }

    /**
     * Creates and registers a new item in the registry using the provided builder.
     *
     * @param name The unique name to associate with the item.
     * @param tags An optional map of tags to associate with the item.
     * @param metadata An optional map of metadata to associate with the item.
     * @param builder An optional configuration builder block.
     * @return The newly created item.
     * @throws IllegalArgumentException If the name is invalid or if an item with the same name already exists.
     * @since 1.0.0
     */
    public suspend fun create(
        name: String,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap(),
        builder: (B.() -> Unit)? = null
    ): C {
        validateName(name)
        val newEntry = withLock { map ->
            if (existsUnsafe(map, name)) {
                throw IllegalArgumentException("Item with name '$name' already exists in the registry")
            }
            val item = create(builder)
            val entry = ResilienceRegistryEntry(item, tags, metadata)
            putUnsafe(map, name, entry)
            entry
        }
        eventBus.push(ResilienceRegistryEvent.EntryAdded(name, newEntry))
        return newEntry.component
    }

    /**
     * Adds an item to the registry, replacing any existing item with the same name.
     *
     * If an item already exists, it is disposed of before being replaced.
     *
     * @param name The unique name to associate with the item.
     * @param item The item instance to store.
     * @param tags An optional map of tags to associate with the item.
     * @param metadata An optional map of metadata to associate with the item.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend fun replace(
        name: String,
        item: C,
        tags: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ) {
        validateName(name)
        val newEntry = ResilienceRegistryEntry(item, tags, metadata)
        val oldEntry = withLock { map ->
            val existing = map[name]
            map[name] = newEntry
            existing
        }
        if (oldEntry != null) {
            eventBus.push(ResilienceRegistryEvent.EntryReplaced(name, oldEntry, newEntry))
            oldEntry.component.dispose()
        } else {
            eventBus.push(ResilienceRegistryEvent.EntryAdded(name, newEntry))
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
     * Operator for adding or replacing an item in the registry using the indexing operator.
     *
     * @param name The name of the item to add or replace.
     * @param item The item to add or replace.
     * @throws IllegalArgumentException If the name is invalid.
     * @since 1.0.0
     */
    public suspend operator fun set(name: String, item: C) {
        replace(name, item)
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
        const val EVENT_TOPIC_NAME = "events"
        val NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._:-]*$")
    }
}
