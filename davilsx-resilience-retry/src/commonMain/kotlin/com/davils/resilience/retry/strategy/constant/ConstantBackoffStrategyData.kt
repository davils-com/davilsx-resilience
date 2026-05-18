package com.davils.resilience.retry.strategy.constant

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import kotlin.time.Duration

/**
 * Data class containing the configuration for a constant backoff strategy.
 *
 * This class is typically instantiated via [ConstantBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ConstantBackoffStrategyData internal constructor(
    /**
     * The fixed delay duration between retry attempts.
     *
     * @since 1.0.0
     */
    val delay: Duration
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (delay.isNegative()) {
            fail("delay must be non-negative", "delay")
        }
    }
}
