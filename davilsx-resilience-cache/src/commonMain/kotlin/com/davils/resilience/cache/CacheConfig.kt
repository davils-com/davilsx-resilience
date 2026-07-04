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

import com.davils.kore.annotation.KoreDsl
import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for write-back cache behavior.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class WriteBackConfig internal constructor(
    /**
     * The interval at which buffered writes are flushed to the backing store.
     *
     * Must be positive when write-back is enabled.
     *
     * @since 1.0.0
     */
    public val flushInterval: Duration,

    /**
     * The number of dirty entries that triggers an immediate flush.
     *
     * Must be at least 1.
     *
     * @since 1.0.0
     */
    public val batchSize: Int,

    /**
     * Whether buffered writes are flushed when the cache is disposed.
     *
     * @since 1.0.0
     */
    public val flushOnDispose: Boolean,
) : DslVerifiableData {
    /**
     * Validates the write-back configuration.
     *
     * @return A [DslVerification] object containing any validation errors.
     * @since 1.0.0
     */
    override fun validate(): DslVerification = verifyDsl {
        if (flushInterval <= Duration.ZERO) {
            fail("flushInterval must be positive", "flushInterval")
        }

        if (batchSize < 1) {
            fail("batchSize must be at least 1", "batchSize")
        }
    }
}

/**
 * Builder for creating instances of [WriteBackConfig].
 *
 * @since 1.0.0
 */
@KoreDsl
public class WriteBackConfigBuilder internal constructor() : DslValidator<WriteBackConfig>() {
    /**
     * The interval at which buffered writes are flushed to the backing store.
     *
     * @since 1.0.0
     */
    public var flushInterval: Duration = 5.seconds

    /**
     * The number of dirty entries that triggers an immediate flush.
     *
     * @since 1.0.0
     */
    public var batchSize: Int = 100

    /**
     * Whether buffered writes are flushed when the cache is disposed.
     *
     * @since 1.0.0
     */
    public var flushOnDispose: Boolean = true

    /**
     * Sets the flush interval.
     *
     * @param flushInterval The interval between flushes.
     * @since 1.0.0
     */
    public fun flushInterval(flushInterval: Duration) {
        this.flushInterval = flushInterval
    }

    /**
     * Sets the flush interval in milliseconds.
     *
     * @param flushIntervalMillis The interval between flushes in milliseconds.
     * @since 1.0.0
     */
    public fun flushInterval(flushIntervalMillis: Long) {
        this.flushInterval = flushIntervalMillis.milliseconds
    }

    /**
     * Sets the batch size that triggers an immediate flush.
     *
     * @param batchSize The batch size.
     * @since 1.0.0
     */
    public fun batchSize(batchSize: Int) {
        this.batchSize = batchSize
    }

    /**
     * Sets whether buffered writes are flushed on dispose.
     *
     * @param flushOnDispose Whether to flush on dispose.
     * @since 1.0.0
     */
    public fun flushOnDispose(flushOnDispose: Boolean) {
        this.flushOnDispose = flushOnDispose
    }

    override fun data(): WriteBackConfig = WriteBackConfig(
        flushInterval = flushInterval,
        batchSize = batchSize,
        flushOnDispose = flushOnDispose,
    )
}

/**
 * Creates a [WriteBackConfig] using the provided configuration [builder].
 *
 * @param builder The configuration builder block.
 * @return A configured [WriteBackConfig] instance.
 * @since 1.0.0
 */
public fun writeBackConfig(builder: WriteBackConfigBuilder.() -> Unit): WriteBackConfig {
    val writeBackConfigBuilder = WriteBackConfigBuilder()
    writeBackConfigBuilder.apply(builder)
    return writeBackConfigBuilder.produce()
}
