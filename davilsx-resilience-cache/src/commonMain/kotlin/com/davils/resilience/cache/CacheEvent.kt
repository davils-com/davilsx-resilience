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

package com.davils.resilience.cache

import com.davils.kore.pattern.reactive.event.EventMarker

/**
 * Base class for all events emitted by a [Cache] instance.
 *
 * @since 1.0.0
 */
public sealed class CacheEvent : EventMarker() {
    /**
     * Emitted when the cache instance is disposed.
     *
     * @since 1.0.0
     */
    public data object CacheDispose : CacheEvent()

    /**
     * Emitted when a cache lookup finds a valid entry.
     *
     * @param key The key that was found.
     * @since 1.0.0
     */
    public data class CacheHit<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when a cache lookup does not find a valid entry.
     *
     * @param key The key that was missed.
     * @since 1.0.0
     */
    public data class CacheMiss<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when a value is stored in the cache.
     *
     * @param key The key that was stored.
     * @since 1.0.0
     */
    public data class CachePut<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when a value is removed from the cache.
     *
     * @param key The key that was removed.
     * @since 1.0.0
     */
    public data class CacheRemove<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when an entry is evicted due to capacity constraints.
     *
     * @param key The key that was evicted.
     * @since 1.0.0
     */
    public data class CacheEviction<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when an entry is removed because it expired.
     *
     * @param key The key that expired.
     * @since 1.0.0
     */
    public data class CacheExpiration<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when a value is successfully loaded from the backing store.
     *
     * @param key The key that was loaded.
     * @since 1.0.0
     */
    public data class CacheLoadSuccess<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when loading a value from the backing store fails.
     *
     * @param key The key that failed to load.
     * @param throwable The exception that occurred during loading.
     * @since 1.0.0
     */
    public data class CacheLoadFailure<K>(public val key: K, public val throwable: Throwable) : CacheEvent()

    /**
     * Emitted when a value is successfully written to the backing store.
     *
     * @param key The key that was written.
     * @since 1.0.0
     */
    public data class CacheWriteSuccess<K>(public val key: K) : CacheEvent()

    /**
     * Emitted when writing a value to the backing store fails.
     *
     * @param key The key that failed to write.
     * @param throwable The exception that occurred during writing.
     * @since 1.0.0
     */
    public data class CacheWriteFailure<K>(public val key: K, public val throwable: Throwable) : CacheEvent()

    /**
     * Emitted when buffered write-back entries are flushed to the backing store.
     *
     * @param count The number of entries flushed.
     * @since 1.0.0
     */
    public data class CacheWriteBackFlushed(public val count: Int) : CacheEvent()

    /**
     * Emitted when all entries are removed from the cache.
     *
     * @since 1.0.0
     */
    public data object CacheCleared : CacheEvent()
}
