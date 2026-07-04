# Circuit Breaker API reference

All circuit breaker operations are **suspend functions** and must be called from a coroutine. Instances are thread-safe and can be shared across coroutines.

## Factory functions

### `circuitBreaker(builder)`

```kotlin
public fun circuitBreaker(builder: CircuitBreakerBuilder.() -> Unit): CircuitBreaker
```

Creates and validates a circuit breaker instance.

### `circuitBreakerRegistry(builder)`

```kotlin
public fun circuitBreakerRegistry(builder: ResilienceRegistryBuilder.() -> Unit): CircuitBreakerRegistry
```

Creates a registry for named `CircuitBreaker` instances.

## CircuitBreaker operations

### Execute

```kotlin
public suspend fun <T> execute(block: suspend () -> T): T
```

Runs [block] inside the circuit breaker. If the circuit is `OPEN` or `FORCED_OPEN`, throws [CallNotPermittedException] without invoking the block.

On success, the result is passed to `onResult`. On failure (non-cancellation), the throwable is passed to `onError` and rethrown.

> **Type inference tip:** When the return value matters for `recordResult`, specify the type explicitly if the call site is in a `Unit` context (e.g. inside `repeat { }`):
>
> ```kotlin
> repeat(4) { cb.execute<String> { "FAILURE_RESULT" } }
> ```

### Permission lifecycle

Use these when you manage the call lifecycle yourself instead of `execute`:

| Method | Description |
|--------|-------------|
| `tryAcquirePermission(): Boolean` | Returns `true` if a call is permitted. For `OPEN`, transitions to `HALF_OPEN` when wait elapsed |
| `acquirePermission()` | Same as above but throws `CallNotPermittedException` when not permitted |
| `releasePermission()` | Returns a HALF_OPEN probe slot without recording an outcome (e.g. on cancellation) |

### Manual outcome recording

Record outcomes without wrapping a block:

| Method | Description |
|--------|-------------|
| `onSuccess(duration)` | Record a successful call |
| `onError(duration, throwable)` | Classify exception via predicates; record success, failure, or ignore |
| `onResult(duration, result)` | Classify result via `recordResult` predicate |

### State queries

| Method | Returns |
|--------|---------|
| `getState(): CircuitBreakerState` | Current state |
| `getMetrics(): CircuitBreakerMetrics` | Metrics snapshot |

### Manual state transitions

| Method | Target state | Notes |
|--------|--------------|-------|
| `reset()` | `CLOSED` | Clears metrics; emits `Reset` |
| `transitionToClosed()` | `CLOSED` | Fresh closed handler |
| `transitionToOpen()` | `OPEN` | Increments open attempt counter |
| `transitionToHalfOpen()` | `HALF_OPEN` | Starts probe quota |
| `transitionToDisabled()` | `DISABLED` | Bypasses protection |
| `transitionToForcedOpen()` | `FORCED_OPEN` | Blocks all calls |
| `transitionToMetricsOnly()` | `METRICS_ONLY` | Records metrics without enforcement |

### Lifecycle

| Method | Description |
|--------|-------------|
| `dispose()` | Publishes `Disposed`, closes event bus |
| `isDisposed(): Boolean` | Whether the instance has been disposed |

Inherited from `ResilienceComponent`:

```kotlin
public fun <R : EventMarker> subscribe(
    eventType: KClass<R>,
    onError: (suspend (Throwable) -> Unit)? = null,
    on: suspend (R) -> Unit
): Job

public inline fun <reified R : EventMarker> subscribe(
    noinline onError: (suspend (Throwable) -> Unit)? = null,
    noinline on: suspend (R) -> Unit
): Job
```

## CircuitBreakerMetrics

Snapshot of metrics at the time of the call:

| Field | Description |
|-------|-------------|
| `numberOfBufferedCalls` | Calls in the current window |
| `numberOfSuccessfulCalls` | Successful calls in the window |
| `numberOfFailedCalls` | Failed calls in the window |
| `numberOfSlowCalls` | Slow calls (success + failure) |
| `numberOfSlowSuccessfulCalls` | Slow successful calls |
| `numberOfSlowFailedCalls` | Slow failed calls |
| `numberOfNotPermittedCalls` | Rejected calls while OPEN/FORCED_OPEN |
| `failureRate` | Percentage (0–100), or `-1` if below minimum calls |
| `slowCallRate` | Percentage (0–100), or `-1` if below minimum calls |

In `OPEN` state, window metrics are frozen from the snapshot taken at transition time; `numberOfNotPermittedCalls` continues to increment.

## Events

All events extend `CircuitBreakerEvent`:

| Event | When emitted |
|-------|--------------|
| `Success(duration)` | Call recorded as success |
| `Error(duration, throwable)` | Call recorded as failure |
| `IgnoredError(duration, throwable)` | Exception matched ignore predicate |
| `CallNotPermitted(state)` | Call rejected (OPEN/FORCED_OPEN) |
| `StateTransition(from, to)` | Any state change |
| `Reset` | After `reset()` |
| `FailureRateExceeded(failureRate)` | Failure threshold breached (before OPEN) |
| `SlowCallRateExceeded(slowCallRate)` | Slow-call threshold breached (before OPEN) |
| `Disposed` | Instance disposed |

Subscribe to specific event types:

```kotlin
cb.subscribe<CircuitBreakerEvent.StateTransition> { (from, to) ->
    println("$from → $to")
}
```

## Exceptions

### `CallNotPermittedException`

Thrown when a call is rejected. Carries the current `state`:

```kotlin
public class CallNotPermittedException(
    public val state: CircuitBreakerState,
    message: String = "CircuitBreaker is $state — call not permitted"
) : Exception(message)
```

### `ResultRecordedAsFailureException`

Internal wrapper used when `recordResult` classifies a successful return value as a failure. Appears as the `throwable` in `Error` events:

```kotlin
public class ResultRecordedAsFailureException(
    public val result: Any?,
    message: String = "Result was recorded as a failure by the circuit breaker"
) : Exception(message)
```

## Registry

`CircuitBreakerRegistry` extends `ResilienceRegistry` and provides thread-safe management of named instances.

Common operations:

| Method | Description |
|--------|-------------|
| `default { }` | Set shared builder defaults |
| `create(name, builder?)` | Create a named instance |
| `getOrCreate(name, builder?)` | Lookup or create |
| `lookup(name)` / `lookupOrNull(name)` | Get existing instance |
| `replace(name, component)` | Replace an instance |
| `remove(name)` / `clear()` | Remove instances |
| `names()` / `values()` / `entries()` | Introspection |

Registry names must match the pattern enforced by `ResilienceRegistry` (alphanumeric with limited punctuation).

```kotlin
val registry = circuitBreakerRegistry {
  default {
    failureRateThreshold = 50f
    slidingWindowSize = 20
    minimumNumberOfCalls = 10
  }
}

val paymentCb = registry.create("payment-service") {
  failureRateThreshold = 30f  // override default
}

val cb = registry.lookup("payment-service")
```

## Types reference

### `CircuitBreakerState`

`CLOSED`, `OPEN`, `HALF_OPEN`, `DISABLED`, `FORCED_OPEN`, `METRICS_ONLY`

### `SlidingWindowType`

`COUNT_BASED`, `TIME_BASED`

### Predicates

```kotlin
public fun interface ExceptionPredicate {
    public fun test(throwable: Throwable): Boolean
}

public fun interface ResultPredicate {
    public fun test(result: Any?): Boolean
}
```

### Wait interval strategies

```kotlin
public fun interface WaitIntervalStrategy {
    public fun waitDuration(attempts: Int): Duration
}

public fun fixedWaitInterval(duration: Duration): WaitIntervalStrategy
public fun exponentialWaitInterval(
    initialDuration: Duration = 60.seconds,
    multiplier: Double = 2.0,
    maxDuration: Duration = Duration.INFINITE,
): WaitIntervalStrategy
```
