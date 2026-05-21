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

    private suspend inline fun <R> withLock(
        block: (MutableMap<String, T>) -> R
    ): R = mutex.withLock {
        block(registry)
    }

    private fun putUnsafe(map: MutableMap<String, T>, name: String, item: T): Boolean {
        if (map.containsKey(name)) return false
        map[name] = item
        return true
    }

    private fun removeUnsafe(map: MutableMap<String, T>, name: String): T? {
        return map.remove(name)
    }

    private fun lookupUnsafe(map: MutableMap<String, T>, name: String): T? {
        return map[name]
    }

    private fun existsUnsafe(map: MutableMap<String, T>, name: String): Boolean {
        return map.containsKey(name)
    }

    private fun validateName(name: String) {
        require(name.matches(NAME_REGEX)) { "Registry item name must match regex: $NAME_REGEX" }
    }

    public suspend fun put(name: String, item: T): Boolean {
        validateName(name)
        return withLock { map ->
            putUnsafe(map, name, item)
        }
    }

    public suspend fun put(item: RegistryItem<T>): Boolean = put(item.name, item.item)

    public suspend fun putIf(name: String, item: T, condition: (name: String, item: T) -> Boolean): Boolean {
        validateName(name)
        return withLock { map ->
            if (!condition(name, item)) return@withLock false
            putUnsafe(map, name, item)
        }
    }

    public suspend fun putAll(items: Map<String, T>) {
        val entries = items.toList()
        entries.forEach { (name, _) ->
            validateName(name)
        }

        withLock { map ->
            val existing = map.keys
            if (entries.any { it.first in existing }) {
                throw IllegalArgumentException("Items already exist")
            }

            map.putAll(entries.toMap())
        }
    }

    public suspend fun putAll(items: Iterable<RegistryItem<T>>) {
        putAll(items.associate { it.name to it.item })
    }

    public suspend fun putAll(vararg items: RegistryItem<T>) {
        putAll(items.asIterable())
    }

    public suspend fun lookupOrNull(name: String): T? {
        return withLock { map ->
            lookupUnsafe(map, name)
        }
    }

    public suspend fun lookup(name: String): T {
        return lookupOrNull(name) ?: throw IllegalArgumentException("No item with name $name found in the registry")
    }

    public suspend fun exists(name: String): Boolean {
        return withLock { map ->
            existsUnsafe(map, name)
        }
    }

    public suspend fun remove(name: String) {
        val removed = withLock { map ->
            removeUnsafe(map, name)
        }
        removed?.dispose()
    }

    public suspend fun removeAll(names: Iterable<String>) {
        val removedItems = withLock { map ->
            names.mapNotNull { removeUnsafe(map, it) }
        }
        removedItems.forEach { it.dispose() }
    }

    public suspend fun removeAll(vararg names: String) {
        removeAll(names.asIterable())
    }

    public suspend fun clear() {
        val removedItems = withLock { map ->
            val items = map.values.toList()
            map.clear()
            items
        }

        removedItems.forEach { it.dispose() }
    }

    public suspend fun removeIf(
        name: String,
        condition: (T) -> Boolean
    ): Boolean {
        val removed = withLock { map ->
            val item = lookupUnsafe(map, name) ?: return@withLock null
            if (!condition(item)) return@withLock null
            removeUnsafe(map, name)
        }

        removed?.dispose()
        return removed != null
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
        val NAME_REGEX = Regex("^[a-z]+(-[a-z]+)*$")
    }
}
