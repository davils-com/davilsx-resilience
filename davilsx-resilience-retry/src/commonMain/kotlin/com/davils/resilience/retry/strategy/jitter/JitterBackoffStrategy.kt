package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.random.Random
import kotlin.time.Duration

/**
 * A backoff strategy that adds random variation (jitter) to another backoff strategy.
 *
 * Jitter helps to prevent "thundering herd" problems where many clients retry
 * at the exact same time, potentially overwhelming the system. It applies a random
 * factor to the delay calculated by the underlying [BackoffStrategy].
 *
 * The delay is calculated as: `baseDelay * random(1 - factor, 1 + factor)`.
 *
 * @since 1.0.0
 */
public class JitterBackoffStrategy internal constructor(private val data: JitterBackoffStrategyData) : BackoffStrategy {
    /**
     * Calculates the jittered delay for the given retry attempt.
     *
     * @param attempt The current retry attempt number.
     * @return The duration to wait before the next attempt, including random jitter.
     * @since 1.0.0
     */
    override fun calculateDelay(attempt: Int): Duration {
        val baseDelay = data.backoffStrategy.calculateDelay(attempt)
        val randomFactor = Random.nextDouble(1.0 - data.factor, 1.0 + data.factor)
        return baseDelay * randomFactor
    }
}

/**
 * Creates a [BackoffStrategy] that adds jitter to the provided [backoffStrategy].
 *
 * This function provides a DSL-like interface to configure and instantiate
 * a [JitterBackoffStrategy].
 *
 * @param backoffStrategy The base strategy to which jitter will be applied.
 * @param builder A lambda to configure the [JitterBackoffStrategyBuilder].
 * @return A [BackoffStrategy] that uses jitter.
 * @since 1.0.0
 */
public fun jitterBackoff(
    backoffStrategy: BackoffStrategy,
    builder: JitterBackoffStrategyBuilder.() -> Unit = {}
): BackoffStrategy {
    val jitterBuilder = JitterBackoffStrategyBuilder(backoffStrategy = backoffStrategy)
    jitterBuilder.builder()
    val data = jitterBuilder.build()
    return JitterBackoffStrategy(data)
}

/**
 * Decorates this [BackoffStrategy] with jitter.
 *
 * @param builder A lambda to configure the [JitterBackoffStrategyBuilder].
 * @return A [BackoffStrategy] that adds jitter to the current strategy.
 * @since 1.0.0
 */
public fun BackoffStrategy.withJitter(
    builder: JitterBackoffStrategyBuilder.() -> Unit = {}
): BackoffStrategy = jitterBackoff(this, builder)

/**
 * Decorates this [BackoffStrategy] with a specific jitter factor.
 *
 * @param factor The jitter factor to apply.
 * @return A [BackoffStrategy] that adds jitter to the current strategy.
 * @since 1.0.0
 */
public fun BackoffStrategy.withJitter(factor: Double): BackoffStrategy = jitterBackoff(this) {
    this.factor = factor
}
