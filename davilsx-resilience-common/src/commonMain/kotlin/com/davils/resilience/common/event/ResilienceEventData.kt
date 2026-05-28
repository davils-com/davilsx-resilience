package com.davils.resilience.common.event

import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Data class containing configuration for resilience event handling.
 *
 * This class holds the parameters necessary to configure how events are processed,
 * buffered, and how errors are handled within the resilience components.
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ResilienceEventData internal constructor(
    /**
     * The coroutine scope used for event processing and collection.
     *
     * @since 1.0.0
     */
    public val scope: CoroutineScope,

    /**
     * A suspending callback triggered when an error occurs during event processing.
     *
     * @since 1.0.0
     */
    public val onError: suspend (Throwable) -> Unit,

    /**
     * The number of values replayed to new subscribers.
     *
     * Must be non-negative.
     *
     * @since 1.0.0
     */
    public val replay: Int,

    /**
     * The strategy to use when the event buffer overflows.
     *
     * @since 1.0.0
     */
    public val overflowStrategy: BufferOverflow,

    /**
     * The additional capacity for the event buffer.
     *
     * Must be non-negative.
     *
     * @since 1.0.0
     */
    public val extraBufferCapacity: Int
) : DslVerifiableData {
    /**
     * Validates the resilience event data configuration.
     *
     * Ensures that [replay] and [extraBufferCapacity] are non-negative.
     *
     * @return A [DslVerification] object containing the result of the validation.
     * @since 1.0.0
     */
    override fun validate(): DslVerification = verifyDsl {
        if (replay < 0) {
            fail("replay must be non-negative", "replay")
        }

        if (extraBufferCapacity < 0) {
            fail("extraBufferCapacity must be non-negative", "extraBufferCapacity")
        }
    }
}
