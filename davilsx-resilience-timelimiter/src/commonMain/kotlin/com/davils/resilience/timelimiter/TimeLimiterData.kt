package com.davils.resilience.timelimiter

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import com.davils.resilience.common.event.ResilienceEventData
import kotlin.time.Duration

@ConsistentCopyVisibility
public data class TimeLimiterData internal constructor(
    val timeout: Duration,
    val cancelOnTimeout: Boolean,
    val strategy: TimeoutStrategy,
    val fallback: (suspend (Throwable) -> Any?)?,
    val eventData: ResilienceEventData
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (timeout.isNegative()) {
            fail("timeout must be non-negative", "timeout")
        }
    }
}
