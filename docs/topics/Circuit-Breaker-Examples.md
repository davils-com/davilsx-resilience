# Circuit Breaker examples

Practical recipes for common circuit breaker scenarios.

## Basic protection

Wrap a remote call and handle rejection:

```kotlin
import com.davils.resilience.circuitbreaker.circuitBreaker
import com.davils.resilience.circuitbreaker.exception.CallNotPermittedException
import kotlin.time.Duration.Companion.seconds

val cb = circuitBreaker {
    failureRateThreshold = 50f
    slidingWindowSize = 10
    minimumNumberOfCalls = 5
    waitDurationInOpenState(10.seconds)
}

suspend fun fetchUser(id: String): User? {
    return try {
        cb.execute { userApi.get(id) }
    } catch (e: CallNotPermittedException) {
        null  // fallback when circuit is open
    }
}
```

## Aggressive protection for a flaky dependency

Tight window and low threshold for fast tripping:

```kotlin
val cb = circuitBreaker {
    failureRateThreshold = 49f
    slidingWindowSize = 4
    minimumNumberOfCalls = 4
    permittedCallsInHalfOpenState = 2
    waitDurationInOpenState(300.milliseconds)
}
```

With 4 consecutive failures the circuit opens immediately (100% failure rate ≥ 49%).

## Slow-call detection

Open the circuit when responses are too slow, even if they succeed:

```kotlin
import kotlin.time.Duration.Companion.milliseconds

val cb = circuitBreaker {
    slowCallRateThreshold = 50f
    slowCallDurationThreshold = 500.milliseconds
    slidingWindowSize = 20
    minimumNumberOfCalls = 10
    failureRateThreshold = 100f  // only slow calls matter
}
```

## Time-based sliding window

Track failures over a rolling time period instead of a fixed call count:

```kotlin
val cb = circuitBreaker {
    slidingWindowType = SlidingWindowType.TIME_BASED
    slidingWindowSize = 60        // last 60 seconds
    minimumNumberOfCalls = 20
    failureRateThreshold = 30f
    waitDurationInOpenState(30.seconds)
}
```

## Ignore client errors

Do not count 4xx-style validation errors as downstream failures:

```kotlin
val cb = circuitBreaker {
    failureRateThreshold = 50f
    slidingWindowSize = 20
    minimumNumberOfCalls = 10
    ignoreException { on<IllegalArgumentException>() }
}
```

Ignored exceptions emit `IgnoredError` events and do not affect the sliding window.

## Record only specific exceptions

Count only infrastructure failures:

```kotlin
val cb = circuitBreaker {
    recordException {
        on<IOException>()
        on<SocketException>()
        includeCauseChain()
    }
}
```

Exceptions that do not match are treated as successes.

## Classify HTTP-style results as failures

Treat error payloads in a 200 response as failures:

```kotlin
data class ApiResponse(val status: String, val body: String?)

val cb = circuitBreaker {
    failureRateThreshold = 50f
    slidingWindowSize = 10
    minimumNumberOfCalls = 5
    recordResult { result ->
        result is ApiResponse && result.status == "ERROR"
    }
}

// Important: specify the return type when the call site is in a Unit context
cb.execute<ApiResponse> { api.fetch() }
```

## Exponential backoff in OPEN state

Increase wait time after repeated failures:

```kotlin
import com.davils.resilience.circuitbreaker.strategy.exponentialWaitInterval
import kotlin.time.Duration.Companion.seconds

val cb = circuitBreaker {
    failureRateThreshold = 50f
    slidingWindowSize = 10
    minimumNumberOfCalls = 5
    waitIntervalInOpenState = exponentialWaitInterval(
        initialDuration = 1.seconds,
        multiplier = 2.0,
        maxDuration = 30.seconds,
    )
}
```

Wait durations: 1s → 2s → 4s → 8s → … capped at 30s.

## Automatic OPEN → HALF_OPEN

Transition without waiting for the next call attempt:

```kotlin
val cb = circuitBreaker {
    waitDurationInOpenState(5.seconds)
    automaticTransitionFromOpenToHalfOpen = true
}
```

A background coroutine transitions to `HALF_OPEN` after the wait duration elapses.

## Event-driven monitoring

Log state changes and threshold breaches:

```kotlin
import com.davils.resilience.circuitbreaker.CircuitBreakerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

val cb = circuitBreaker {
    failureRateThreshold = 50f
    slidingWindowSize = 10
    minimumNumberOfCalls = 5
    event { scope = CoroutineScope(Dispatchers.Default) }
}

cb.subscribe<CircuitBreakerEvent.StateTransition> { (from, to) ->
    logger.info { "Circuit $from → $to" }
}

cb.subscribe<CircuitBreakerEvent.FailureRateExceeded> { (rate) ->
    logger.warn { "Failure rate exceeded: $rate%" }
}

cb.subscribe<CircuitBreakerEvent.CallNotPermitted> { event ->
    metrics.increment("circuit.rejected", tags = mapOf("state" to event.state.name))
}
```

## Registry with per-service breakers

Share defaults, customize per downstream:

```kotlin
import com.davils.resilience.circuitbreaker.circuitBreakerRegistry

val registry = circuitBreakerRegistry {
    default {
        failureRateThreshold = 50f
        slidingWindowSize = 100
        minimumNumberOfCalls = 50
        waitDurationInOpenState(30.seconds)
    }
}

val paymentCb = registry.create("payment-api") {
    failureRateThreshold = 30f  // stricter for payments
}

val analyticsCb = registry.create("analytics-api") {
    failureRateThreshold = 70f  // more tolerant
}

suspend fun charge(amount: Money) {
    registry.lookup("payment-api").execute { paymentClient.charge(amount) }
}
```

## Manual operations

### Kill switch during maintenance

```kotlin
cb.transitionToForcedOpen()
// all calls blocked until:
cb.transitionToClosed()
```

### Observe without blocking

```kotlin
cb.transitionToMetricsOnly()
// calls pass through, metrics collected, thresholds not enforced
```

### Reset after deployment

```kotlin
cb.reset()  // CLOSED, metrics cleared, Reset event emitted
```

## HALF_OPEN probe flow

Typical recovery sequence:

```kotlin
// 1. Circuit opens after failures
repeat(5) { runCatching { cb.execute { failingService() } } }
check(cb.getState() == CircuitBreakerState.OPEN)

// 2. Wait for open duration
delay(6.seconds)

// 3. Next call triggers OPEN → HALF_OPEN and runs as a probe
val result = cb.execute { recoveringService() }

// 4. If all probes succeed, circuit closes automatically
check(cb.getState() == CircuitBreakerState.CLOSED)
```

## Composing with manual permission control

For frameworks that manage their own execution:

```kotlin
if (cb.tryAcquirePermission()) {
    try {
        val result = doWork()
        cb.onResult(duration, result)
    } catch (e: Exception) {
        cb.onError(duration, e)
        throw e
    }
} else {
    // handle rejection
}
```

On coroutine cancellation in HALF_OPEN, `execute` releases the probe permit automatically via `releasePermission()`.

## Full runnable demo

The `example-circuitbreaker` module demonstrates:

- Driving a circuit open with repeated failures
- Waiting for OPEN → HALF_OPEN transition
- Probe calls and recovery to CLOSED
- Event subscription for state transitions
- Exponential wait interval calculation

Run it from the `davilsx-resilience-example/example-circuitbreaker` module.
