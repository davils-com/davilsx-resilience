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

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A cache entry containing a value and metadata used for TTL and eviction.
 *
 * @param V The type of the cached value.
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class CacheEntry<V> internal constructor(
    /**
     * The cached value.
     *
     * @since 1.0.0
     */
    public val value: V,

    /**
     * The time when this entry was created.
     *
     * @since 1.0.0
     */
    public val createdAt: TimeMark,

    /**
     * The time when this entry was last accessed.
     *
     * @since 1.0.0
     */
    public val lastAccessedAt: TimeMark,

    /**
     * The number of times this entry has been accessed.
     *
     * @since 1.0.0
     */
    public val accessCount: Long,

    /**
     * The insertion sequence number used for FIFO eviction.
     *
     * @since 1.0.0
     */
    public val insertionSeq: Long,
) {
    /**
     * Returns a copy of this entry with updated access metadata.
     *
     * @return A new [CacheEntry] with an incremented access count and refreshed last access time.
     * @since 1.0.0
     */
    public fun accessed(): CacheEntry<V> {
        val now = TimeSource.Monotonic.markNow()
        return copy(
            lastAccessedAt = now,
            accessCount = accessCount + 1,
        )
    }

    /**
     * Checks whether this entry has expired based on the given TTL settings.
     *
     * @param expireAfterWrite The maximum age of the entry since creation. [Duration.ZERO] disables write expiry.
     * @param expireAfterAccess The maximum idle time since last access. [Duration.ZERO] disables access expiry.
     * @return `true` if the entry is expired, `false` otherwise.
     * @since 1.0.0
     */
    public fun isExpired(
        expireAfterWrite: Duration,
        expireAfterAccess: Duration,
    ): Boolean {
        if (expireAfterWrite > Duration.ZERO && createdAt.elapsedNow() >= expireAfterWrite) {
            return true
        }

        if (expireAfterAccess > Duration.ZERO && lastAccessedAt.elapsedNow() >= expireAfterAccess) {
            return true
        }

        return false
    }

    internal companion object {
        internal fun <V> create(value: V, insertionSeq: Long): CacheEntry<V> {
            val now = TimeSource.Monotonic.markNow()
            return CacheEntry(
                value = value,
                createdAt = now,
                lastAccessedAt = now,
                accessCount = 1,
                insertionSeq = insertionSeq,
            )
        }
    }
}
