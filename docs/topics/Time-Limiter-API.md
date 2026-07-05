# Time Limiter — API Reference

## Core operations

| Method | Description |
|--------|-------------|
| `execute(block)` | Run block within the configured timeout |
| `getMetrics()` | Metrics snapshot |
| `changeTimeout(timeout)` | Hot-reload timeout duration |
| `dispose()` | Release resources and close the event bus |
| `isDisposed()` | Check whether the instance is disposed |
| `subscribe<T>(...)` | Subscribe to typed events |

## Events

| Event | When |
|-------|------|
| `TimeoutExceeded(timeoutMs)` | Execution exceeded the configured timeout |
| `TimeLimiterDisposed` | Instance disposed |

## Metrics snapshot

`TimeLimiterMetrics` exposes:

- `timeout`
- `strategy`
- `cancelOnTimeout`
- `numberOfSuccessfulCalls`
- `numberOfTimeoutCalls`
- `totalExecutionTime`

## Registry

```kotlin
val registry = timeLimiterRegistry { }
val api = registry.create("api") {
    timeout = 2.seconds
}
```

## Metrics collector

```kotlin
import com.davils.resilience.metrics.timelimiter.metrics

val snapshot = limiter.metrics.allMetrics()
```

## Ktor plugin

```kotlin
install(TimeLimiterPlugin) {
    timeLimiter = hardTimeLimiter(timeout = 5.seconds)
}
```

Returns HTTP 504 Gateway Timeout when request handling exceeds the limit.
