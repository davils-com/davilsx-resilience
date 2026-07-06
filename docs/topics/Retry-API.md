# Retry — API Reference

## Core operations

| Method | Description |
|--------|-------------|
| `execute(block)` | Run block with automatic retries according to the configured policy |
| `getMetrics()` | Metrics snapshot |
| `subscribe(eventType, handler)` | Listen for retry lifecycle events |
| `dispose()` | Close the event bus and reject further executions |

## Events

| Event | When |
|-------|------|
| `RetryAttemptStarted(attempt)` | Before each attempt |
| `RetryAttemptFailed(attempt, throwable)` | Attempt failed; may retry |
| `RetryAttemptBackoff(attempt, delay)` | Waiting before the next attempt |
| `RetrySucceeded(attempt)` | Operation completed successfully |
| `RetryFailed(attempt, throwable)` | Permanent failure; no more retries |
| `RetryCancelled(attempt, cancellation)` | `CancellationException` from the block |
| `RetryDisposed` | Instance disposed |

## Exceptions

| Type | When |
|------|------|
| `MaxRetriesExceededException` | Result predicate still requests retry at `maxAttempts` with `OnResultExhaustion.THROW` |
| Original `Throwable` | Exception-based exhaustion (last caught exception is rethrown) |

## Metrics snapshot

`RetryMetrics` exposes:

- `totalCalls`, `successfulCalls`, `exhaustedCalls`
- `failedNonRetryableCalls`, `canceledCalls`
- `totalAttempts`, `successfulAttempts`, `failedAttempts`
- `totalCallDuration`, `totalAttemptDuration`, `totalBackoffDuration`
- `callsActive`, `callsWaiting`

## Registry

```kotlin
val registry = retryRegistry { }
val api = registry.create("api") {
    maxAttempts = 5
}
```

## Metrics collector

```kotlin
import com.davils.resilience.metrics.retry.metrics

val snapshot = policy.metrics.allMetrics()
```
