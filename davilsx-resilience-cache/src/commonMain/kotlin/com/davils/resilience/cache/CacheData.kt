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

import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import com.davils.resilience.common.ResilienceComponentData
import com.davils.resilience.common.event.ResilienceEventData
import kotlin.time.Duration

/**
 * Configuration data for a [Cache].
 *
 * @param K The type of cache keys.
 * @param V The type of cache values.
 * @since 1.2.0
 */
@ConsistentCopyVisibility
public data class CacheData<K, V> internal constructor(
    /**
     * The event configuration for the cache.
     *
     * @since 1.2.0
     */
    override val eventData: ResilienceEventData,

    /**
     * The maximum number of entries the cache may hold.
     *
     * Must be at least 1.
     *
     * @since 1.2.0
     */
    public val maxSize: Int,

    /**
     * The strategy used to select entries for eviction.
     *
     * @since 1.2.0
     */
    public val evictionStrategy: EvictionStrategy,

    /**
     * The maximum age of an entry since creation.
     *
     * [Duration.ZERO] disables write-based expiration.
     *
     * @since 1.2.0
     */
    public val expireAfterWrite: Duration,

    /**
     * The maximum idle time of an entry since last access.
     *
     * [Duration.ZERO] disables access-based expiration.
     *
     * @since 1.2.0
     */
    public val expireAfterAccess: Duration,

    /**
     * The optional backing store for read-through and write operations.
     *
     * @since 1.2.0
     */
    public val store: CacheStore<K, V>?,

    /**
     * The write strategy used when persisting values.
     *
     * @since 1.2.0
     */
    public val writeStrategy: WriteStrategy,

    /**
     * The configuration for write-back behavior.
     *
     * @since 1.2.0
     */
    public val writeBackConfig: WriteBackConfig,

    /**
     * The interval at which expired entries are actively removed.
     *
     * [Duration.ZERO] disables active cleanup.
     *
     * @since 1.2.0
     */
    public val cleanupInterval: Duration,
) : ResilienceComponentData {
    /**
     * Validates the cache configuration.
     *
     * @return A [DslVerification] object containing any validation errors.
     * @since 1.2.0
     */
    override fun validate(): DslVerification = verifyDsl {
        if (maxSize < 1) {
            fail("maxSize must be at least 1", "maxSize")
        }

        if (expireAfterWrite.isNegative()) {
            fail("expireAfterWrite must be non-negative", "expireAfterWrite")
        }

        if (expireAfterAccess.isNegative()) {
            fail("expireAfterAccess must be non-negative", "expireAfterAccess")
        }

        if (cleanupInterval.isNegative()) {
            fail("cleanupInterval must be non-negative", "cleanupInterval")
        }

        if (writeStrategy == WriteStrategy.WRITE_BACK) {
            if (store == null) {
                fail("store must be configured when writeStrategy is WRITE_BACK", "store")
            }

            writeBackConfig.validate().failures.forEach { failure ->
                fail(failure.message, failure.field)
            }
        }
    }
}
