package com.davils.resilience.common.event

import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.creational.dsl.verification.DslVerification
import com.davils.kore.pattern.creational.dsl.verification.verifyDsl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow

@ConsistentCopyVisibility
public data class ResilienceEventData internal constructor(
    public val scope: CoroutineScope,
    public val onError: suspend (Throwable) -> Unit,
    public val replay: Int,
    public val overflowStrategy: BufferOverflow,
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
