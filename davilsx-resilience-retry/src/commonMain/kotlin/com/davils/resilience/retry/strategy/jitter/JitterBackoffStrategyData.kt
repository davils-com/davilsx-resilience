package com.davils.resilience.retry.strategy.jitter

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.time.Duration

/**
 * Data class containing the configuration for a jitter backoff strategy.
 *
 * This class is typically instantiated via [JitterBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class JitterBackoffStrategyData internal constructor(
    /**
     * The base backoff strategy to which jitter is applied.
     *
     * @since 1.0.0
     */
    val backoffStrategy: BackoffStrategy,

    /**
     * The jitter factor to apply.
     *
     * Only relevant for [JitterMode.PROPORTIONAL]. For other modes the value is ignored.
     *
     * @since 1.0.0
     */
    val factor: Double,

    /**
     * The jitter algorithm to apply on top of the [backoffStrategy].
     *
     * Defaults to [JitterMode.PROPORTIONAL] for backwards compatibility.
     *
     * @since 1.0.0
     */
    val mode: JitterMode,

    /**
     * The upper bound applied to the jittered delay.
     *
     * Used by [JitterMode.DECORRELATED] to bound the unbounded random growth and may
     * also be used by other modes as a safety cap. Must be strictly positive.
     *
     * Defaults to [Duration.INFINITE], meaning no cap is applied.
     *
     * @since 1.0.0
     */
    val cap: Duration
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (factor > 0.0 && factor <= 1.0) {
            fail("factor must be greater than 0.0 and less than or equal to 1.0", "factor")
        }

        if (cap.isNegative()) {
            fail("cap must be non-negative", "cap")
        }
    }
}
