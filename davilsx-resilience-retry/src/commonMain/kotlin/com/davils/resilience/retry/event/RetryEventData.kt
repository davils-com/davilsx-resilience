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

package com.davils.resilience.retry.event

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Internal data class representing the configuration of the retry event system.
 *
 * Instances are created by [RetryEventBuilder] and used to initialize the
 * internal event bus.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class RetryEventData internal constructor(
    /**
     * The [CoroutineScope] used for event operations.
     *
     * @since 1.0.0
     */
    public val scope: CoroutineScope,

    /**
     * Error handler for the event bus.
     *
     * @since 1.0.0
     */
    public val onError: suspend (Throwable) -> Unit,

    /**
     * Number of events to replay.
     *
     * @since 1.0.0
     */
    public val replay: Int,

    /**
     * Strategy for buffer overflow.
     *
     * @since 1.0.0
     */
    public val overflowStrategy: BufferOverflow,

    /**
     * Additional buffer capacity.
     *
     * @since 1.0.0
     */
    public val extraBufferCapacity: Int
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (replay < 0) {
            fail("replay must be non-negative", "replay")
        }

        if (extraBufferCapacity < 0) {
            fail("extraBufferCapacity must be non-negative", "extraBufferCapacity")
        }
    }
}
