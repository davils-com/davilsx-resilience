package com.davils.resilience.retry.strategy.jitter

import com.davils.resilience.retry.strategy.BackoffStrategy
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A backoff strategy that adds random variation (jitter) to another backoff strategy.
 *
 * Jitter helps to prevent "thundering herd" problems where many clients retry
 * at the exact same time, potentially overwhelming the system. It applies a randomization
 * algorithm to the delay calculated by the underlying [BackoffStrategy].
 *
 * The concrete formula depends on the configured [JitterMode]:
 *  - [JitterMode.PROPORTIONAL]: `baseDelay * random(1 - factor, 1 + factor)`
 *  - [JitterMode.FULL]: `random(0, baseDelay)`
 *  - [JitterMode.EQUAL]: `baseDelay / 2 + random(0, baseDelay / 2)`
 *  - [JitterMode.DECORRELATED]: `min(cap, random(baseDelay, previousDelay * 3))`
 *
 * The result is always clamped to the configured [JitterBackoffStrategyData.cap].
 *
 * Instances are not safe for concurrent use across multiple retry executions when
 * [JitterMode.DECORRELATED] is selected because the algorithm relies on the previously
 * computed delay. Create a separate strategy per retry execution in such cases.
 *
 * @since 1.0.0
 */
public class JitterBackoffStrategy internal constructor(private val data: JitterBackoffStrategyData) : BackoffStrategy {
    /**
     * The previously computed delay, used by [JitterMode.DECORRELATED] to derive the next delay.
     *
     * Initialized to [Duration.ZERO] so the first decorrelated attempt falls back to the base delay.
     */
    private var previousDelay: Duration = Duration.ZERO

    /**
     * Calculates the jittered delay for the given retry attempt.
     *
     * @param attempt The current retry attempt number.
     * @return The duration to wait before the next attempt, including random jitter.
     * @since 1.0.0
     */
    override fun calculateDelay(attempt: Int): Duration {
        val baseDelay = data.backoffStrategy.calculateDelay(attempt)
        val jittered = when (data.mode) {
            JitterMode.PROPORTIONAL -> {
                val randomFactor = Random.nextDouble(1.0 - data.factor, 1.0 + data.factor)
                baseDelay * randomFactor
            }

            JitterMode.FULL -> randomDuration(Duration.ZERO, baseDelay)

            JitterMode.EQUAL -> {
                val half = baseDelay / 2.0
                half + randomDuration(Duration.ZERO, half)
            }

            JitterMode.DECORRELATED -> {
                val previous = if (previousDelay > Duration.ZERO) previousDelay else baseDelay
                val upper = previous * 3.0
                val next = randomDuration(baseDelay, upper)
                next
            }
        }

        val capped = if (jittered > data.cap) data.cap else jittered
        if (data.mode == JitterMode.DECORRELATED) {
            previousDelay = capped
        }

        return capped
    }

    /**
     * Returns a random [Duration] uniformly distributed in `[from, until]`.
     *
     * If `until <= from`, `from` is returned unchanged. The randomization is performed in
     * nanoseconds to preserve precision for sub-millisecond delays.
     */
    private fun randomDuration(from: Duration, until: Duration): Duration {
        if (until <= from) return from
        val fromNanos = from.inWholeNanoseconds
        val untilNanos = until.inWholeNanoseconds
        if (untilNanos <= fromNanos) return from
        return Random.nextLong(fromNanos, untilNanos).nanoseconds
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
    val data = jitterBuilder.produce()
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

/**
 * Decorates this [BackoffStrategy] with full jitter.
 *
 * The resulting delay is uniformly distributed in `[0, baseDelay]`.
 *
 * @param cap The optional upper bound applied to the jittered delay. Defaults to [Duration.INFINITE].
 * @return A [BackoffStrategy] that applies [JitterMode.FULL] jitter to the current strategy.
 * @since 1.0.0
 */
public fun BackoffStrategy.withFullJitter(cap: Duration = Duration.INFINITE): BackoffStrategy = jitterBackoff(this) {
    this.mode = JitterMode.FULL
    this.cap = cap
}

/**
 * Decorates this [BackoffStrategy] with equal jitter.
 *
 * The resulting delay is calculated as `baseDelay / 2 + random(0, baseDelay / 2)`.
 *
 * @param cap The optional upper bound applied to the jittered delay. Defaults to [Duration.INFINITE].
 * @return A [BackoffStrategy] that applies [JitterMode.EQUAL] jitter to the current strategy.
 * @since 1.0.0
 */
public fun BackoffStrategy.withEqualJitter(cap: Duration = Duration.INFINITE): BackoffStrategy = jitterBackoff(this) {
    this.mode = JitterMode.EQUAL
    this.cap = cap
}

/**
 * Decorates this [BackoffStrategy] with decorrelated jitter.
 *
 * The resulting delay is calculated as `min(cap, random(baseDelay, previousDelay * 3))`,
 * where `previousDelay` is tracked by the strategy across attempts.
 *
 * @param cap The upper bound applied to the jittered delay. Defaults to [Duration.INFINITE].
 * @return A [BackoffStrategy] that applies [JitterMode.DECORRELATED] jitter to the current strategy.
 * @since 1.0.0
 */
public fun BackoffStrategy.withDecorrelatedJitter(cap: Duration = Duration.INFINITE): BackoffStrategy = jitterBackoff(this) {
    this.mode = JitterMode.DECORRELATED
    this.cap = cap
}
