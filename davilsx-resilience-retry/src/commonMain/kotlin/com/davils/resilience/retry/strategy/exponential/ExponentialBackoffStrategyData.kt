package com.davils.resilience.retry.strategy.exponential

import com.davils.kore.pattern.dsl.verification.DslVerifiableData
import com.davils.kore.pattern.dsl.verification.DslVerification
import com.davils.kore.pattern.dsl.verification.verifyDsl
import kotlin.time.Duration

/**
 * Data class containing the configuration for an exponential backoff strategy.
 *
 * This class is typically instantiated via [ExponentialBackoffStrategyBuilder].
 *
 * @since 1.0.0
 */
@ConsistentCopyVisibility
public data class ExponentialBackoffStrategyData internal constructor(
    /**
     * The maximum duration to wait between retry attempts.
     *
     * @since 1.0.0
     */
    public val maxDelay: Duration,

    /**
     * The factor by which the delay increases with each attempt.
     *
     * @since 1.0.0
     */
    public val multiplier: Double,

    /**
     * The initial duration to wait before the first retry attempt.
     *
     * @since 1.0.0
     */
    public val initialDelay: Duration
) : DslVerifiableData {
    override fun validate(): DslVerification = verifyDsl {
        if (maxDelay.isNegative()) {
            fail("maxDelay must be non-negative", "maxDelay")
        }

        if (multiplier > 0.0) {
            fail("multiplier must be greater than 0.0", "multiplier")
        }

        if (initialDelay.isNegative()) {
            fail("initialDelay must be non-negative", "initialDelay")
        }

        if (initialDelay > maxDelay) {
            fail("initialDelay must be less than or equal to maxDelay", "initialDelay")
        }
    }
}
