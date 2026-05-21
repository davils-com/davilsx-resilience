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

import com.davils.resilience.common.DisposableAsync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public abstract class Registry<T : DisposableAsync> {
    private val mutex = Mutex()

    private val registry = mutableMapOf<String, T>()

    private suspend inline fun <R> withLock(block: (MutableMap<String, T>) -> R): R = mutex.withLock {
        block(registry)
    }

    public suspend fun put(name: String, item: T): Boolean {
        require(name.matches(nameRegex)) { "Name must match $nameRegex" }

        withLock { map ->
            if (map.containsKey(name)) {
                return false
            }
            map[name] = item
            return true
        }
    }

    public suspend fun put(item: RegistryItem<T>): Boolean = put(item.name, item.item)

    public suspend fun putIfAbsent(name: String, item: T) {
        require(name.matches(nameRegex)) { "Name must match $nameRegex" }

        withLock { map ->
            if (!map.containsKey(name)) {
                map[name] = item
            }
        }
    }

    public suspend fun putIfAbsent(item: RegistryItem<T>) {
        putIfAbsent(item.name, item.item)
    }

    public suspend fun putIf(name: String, item: T, condition: (T) -> Boolean): Boolean {
        withLock { map ->
            require(name.matches(nameRegex)) { "Name must match $nameRegex" }

            if (map.containsKey(name)) {
                return false
            }

            val conditionResult = condition.invoke(item)
            if (!conditionResult) {
                return false
            }

            map[name] = item
            return true
        }
    }

    public suspend fun putAll(items: Map<String, T>) {
        withLock { map ->
            require(items.all { it.key.matches(nameRegex) }) { "All names must match $nameRegex" }

            val duplicateNames = items.keys.intersect(map.keys)
            if (duplicateNames.isNotEmpty()) {
                throw IllegalArgumentException("Items with names ${duplicateNames.joinToString(", ")} already exist in the registry")
            }
            map.putAll(items)
        }
    }

    public suspend fun putAll(items: Iterable<RegistryItem<T>>) {
        putAll(items.associate { it.name to it.item })
    }

    public suspend fun putAll(vararg items: RegistryItem<T>) {
        putAll(items.asIterable())
    }

    public suspend fun putAll(items: Sequence<RegistryItem<T>>) {
        putAll(items.asIterable())
    }

    public suspend fun lookupOrNull(name: String): T? {
        return withLock { map ->
            map[name]
        }
    }

    public suspend fun lookupOrNull(name: String, transform: suspend (T?) -> Unit): T? {
        val lookup = lookupOrNull(name)
        transform.invoke(lookup)
        return lookup
    }

    public suspend fun lookup(name: String): T {
        return lookupOrNull(name) ?: throw IllegalArgumentException("No item with name $name found in the registry")
    }

    public suspend fun lookup(name: String, transform: suspend (T) -> Unit): T {
        val lookup = lookup(name)
        transform.invoke(lookup)
        return lookup
    }

    public suspend fun exists(name: String): Boolean {
        return withLock { map ->
            map.containsKey(name)
        }
    }

    public suspend fun remove(name: String) {
        val removed = withLock { map ->
            map.remove(name)
        }
        removed?.dispose()
    }

    public suspend fun removeAll(names: Iterable<String>) {
        val removedItems = withLock { map ->
            names.mapNotNull { name ->
                map.remove(name)
            }
        }

        removedItems.forEach { item ->
            item.dispose()
        }
    }

    public suspend fun removeAll(names: Sequence<String>) {
        removeAll(names.asIterable())
    }

    public suspend fun removeAll(vararg names: String) {
        removeAll(names.asIterable())
    }

    // TODO: removeIf

    public suspend fun clear() {
        val removedItems = withLock { map ->
            val items = map.values.toList()
            map.clear()
            items
        }

        removedItems.forEach { item ->
            item.dispose()
        }
    }

    public suspend operator fun get(name: String): T {
        return lookupOrNull(name) ?: throw IllegalArgumentException("No item with name $name found in the registry")
    }

    public suspend operator fun plusAssign(item: RegistryItem<T>) {
        put(item)
    }

    public suspend operator fun minusAssign(name: String) {
        remove(name)
    }

    public suspend operator fun String.unaryMinus() {
        remove(this)
    }

    private companion object {
        val nameRegex = Regex("^[a-z]+(-[a-z]+)*$")
    }
}
