# Circuit Breaker configuration

Configure a circuit breaker with the `circuitBreaker { }` DSL. All settings are validated at build time; invalid combinations throw `DslVerificationException`.

```kotlin
import com.davils.resilience.circuitbreaker.*
import com.davils.resilience.circuitbreaker.strategy.exponentialWaitInterval
import kotlin.time.Duration.Companion.seconds

val cb = circuitBreaker {
  failureRateThreshold = 50f
  slowCallRateThreshold = 100f
  slowCallDurationThreshold = 2.seconds
  slidingWindowType = SlidingWindowType.COUNT_BASED
  slidingWindowSize = 100
  minimumNumberOfCalls = 100
  permittedCallsInHalfOpenState = 10
  waitDurationInOpenState(60.seconds)
  automaticTransitionFromOpenToHalfOpen = false
  maxWaitDurationInHalfOpenState = Duration.ZERO
  transitionStateAfterWaitDuration = CircuitBreakerState.OPEN
  initialState = CircuitBreakerState.CLOSED

  recordException { on<IOException>(); includeCauseChain() }
  ignoreException { on<IllegalArgumentException>() }
  recordResult { result -> result == "ERROR" }

  event {
    scope = myCoroutineScope
    replay = 0
    extraBufferCapacity = 128
  }
}
```

## Threshold settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `failureRateThreshold` | `Float` | `50` | Failure rate (0–100%) that opens the circuit when exceeded |
| `slowCallRateThreshold` | `Float` | `100` | Slow-call rate (0–100%) that opens the circuit when exceeded |
| `slowCallDurationThreshold` | `Duration` | `60s` | Calls longer than this are classified as slow |

Rates use `>=` comparison. A 50% threshold opens at exactly 50%.

## Sliding window settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `slidingWindowType` | `SlidingWindowType` | `COUNT_BASED` | `COUNT_BASED` or `TIME_BASED` |
| `slidingWindowSize` | `Int` | `100` | Window size: number of calls (count) or seconds (time) |
| `minimumNumberOfCalls` | `Int` | `100` | Minimum buffered calls before rates are evaluated |

### COUNT_BASED

Uses a fixed-size ring buffer. The window always holds at most `slidingWindowSize` recent calls.

For count-based windows, `minimumNumberOfCalls` must not exceed `slidingWindowSize`. The effective minimum is `min(minimumNumberOfCalls, slidingWindowSize)`.

### TIME_BASED

Retains all calls recorded within the last `slidingWindowSize` seconds. Older entries are evicted automatically.

## OPEN and HALF_OPEN settings

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `waitIntervalInOpenState` | `WaitIntervalStrategy` | fixed `60s` | How long the circuit stays OPEN before HALF_OPEN |
| `waitDurationInOpenState(duration)` | `Duration` | — | Shorthand for `fixedWaitInterval(duration)` |
| `permittedCallsInHalfOpenState` | `Int` | `10` | Probe calls allowed in HALF_OPEN |
| `automaticTransitionFromOpenToHalfOpen` | `Boolean` | `false` | Auto-transition OPEN → HALF_OPEN after wait (no call required) |
| `maxWaitDurationInHalfOpenState` | `Duration` | `0` (disabled) | Max time in HALF_OPEN before forced transition |
| `transitionStateAfterWaitDuration` | `CircuitBreakerState` | `OPEN` | Target state when HALF_OPEN times out (`OPEN` or `CLOSED` only) |
| `initialState` | `CircuitBreakerState` | `CLOSED` | State when the instance is created |

### Wait interval strategies

**Fixed** — same duration on every open:

```kotlin
waitDurationInOpenState(5.seconds)
// equivalent to:
waitIntervalInOpenState = fixedWaitInterval(5.seconds)
```

**Exponential** — grows with consecutive OPEN transitions:

```kotlin
waitIntervalInOpenState = exponentialWaitInterval(
    initialDuration = 1.seconds,
    multiplier = 2.0,
    maxDuration = 30.seconds,
)
```

Formula: `initialDuration × multiplier^(attempts − 1)`, capped at `maxDuration`. The `attempts` counter increments on each OPEN transition.

### OPEN → HALF_OPEN transition modes

| Mode | Setting | Behavior |
|------|---------|----------|
| On next call | `automaticTransitionFromOpenToHalfOpen = false` (default) | `tryAcquirePermission()` checks elapsed wait and transitions |
| Scheduled | `automaticTransitionFromOpenToHalfOpen = true` | Coroutine transitions after wait duration without a call |

## Predicate configuration

### `recordException { }`

Defines which thrown exceptions count as failures.

```kotlin
recordException { on<IOException>() }
recordException { on<SocketException>(); includeCauseChain() }
```

If no types are specified in the builder, **all exceptions** are recorded (default).

You can also pass a predicate directly:

```kotlin
recordException(ExceptionPredicate { it is IOException })
```

### `ignoreException { }`

Defines exceptions that are **not counted at all** — neither as success nor failure. Ignore wins over record.

```kotlin
ignoreException { on<IllegalArgumentException>() }
```

Default: no exceptions ignored.

### `recordResult(predicate)`

Treats matching successful return values as failures:

```kotlin
recordResult { result -> result == "SERVICE_UNAVAILABLE" }
recordResult(ResultPredicate { it is ErrorResponse })
```

When matched, the call is recorded as a failure and an `Error` event is emitted with `ResultRecordedAsFailureException`.

## Event block

Inherited from `ResilienceComponentBuilder`. Configures the internal event bus:

```kotlin
event {
  scope = CoroutineScope(Dispatchers.Default)
  replay = 0
  extraBufferCapacity = 128
  overflowStrategy = BufferOverflow.DROP_OLDEST
  onError = { throwable -> log.error(throwable) { "Event handler failed" } }
}
```

## Validation rules

| Rule | Property |
|------|----------|
| `0 ≤ failureRateThreshold ≤ 100` | `failureRateThreshold` |
| `0 ≤ slowCallRateThreshold ≤ 100` | `slowCallRateThreshold` |
| `slowCallDurationThreshold ≥ 0` | `slowCallDurationThreshold` |
| `permittedCallsInHalfOpenState ≥ 1` | `permittedCallsInHalfOpenState` |
| `minimumNumberOfCalls ≥ 1` | `minimumNumberOfCalls` |
| `slidingWindowSize ≥ 1` | `slidingWindowSize` |
| `minimumNumberOfCalls ≤ slidingWindowSize` (COUNT_BASED only) | `minimumNumberOfCalls` |
| `maxWaitDurationInHalfOpenState ≥ 0` | `maxWaitDurationInHalfOpenState` |
| `transitionStateAfterWaitDuration` is `OPEN` or `CLOSED` | `transitionStateAfterWaitDuration` |

## Registry defaults

Configure shared defaults for all circuit breakers created through a registry:

```kotlin
val registry = circuitBreakerRegistry {
  default {
    failureRateThreshold = 50f
    slidingWindowSize = 20
    minimumNumberOfCalls = 10
  }
}
```

Named instances inherit registry defaults and can override per instance. See [API reference — Registry](Circuit-Breaker-API.md#registry).
