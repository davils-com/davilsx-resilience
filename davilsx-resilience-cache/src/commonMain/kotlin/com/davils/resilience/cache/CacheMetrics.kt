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

/**
 * A snapshot of the current metrics tracked by a [Cache].
 *
 * All values reflect cumulative counters and cache state at the moment the snapshot was taken.
 *
 * @since 1.0.0
 */
public data class CacheMetrics(
    /** Number of cache hits recorded via [Cache.get] and [Cache.get] with a loader. */
    public val numberOfHits: Long,
    /** Number of cache misses recorded via [Cache.get] and [Cache.get] with a loader. */
    public val numberOfMisses: Long,
    /** Number of values stored via [Cache.put]. */
    public val numberOfPuts: Long,
    /** Number of entries removed via [Cache.remove]. */
    public val numberOfRemoves: Long,
    /** Number of entries evicted due to capacity constraints. */
    public val numberOfEvictions: Long,
    /** Number of entries removed due to TTL expiration. */
    public val numberOfExpirations: Long,
    /** Number of successful read-through loads from the backing store. */
    public val numberOfLoadSuccesses: Long,
    /** Number of failed read-through loads from the backing store. */
    public val numberOfLoadFailures: Long,
    /** Number of successful writes to the backing store. */
    public val numberOfWriteSuccesses: Long,
    /** Number of failed writes to the backing store. */
    public val numberOfWriteFailures: Long,
    /** Total number of entries flushed during write-back operations. */
    public val numberOfWriteBackFlushedEntries: Long,
    /** Number of times [Cache.clear] was invoked. */
    public val numberOfClears: Long,
    /** Current number of entries held by the cache. */
    public val currentSize: Long,
    /** Hit rate as a percentage (0–100), or -1 if no lookups were recorded. */
    public val hitRate: Float,
)
