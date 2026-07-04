# Rate Limiter — API Reference

## Core operations

| Method | Description |
|--------|-------------|
| `execute(permits, block)` | Acquire permits, run block, throw on rejection |
| `acquireSlot(permits)` | Acquire with strategy/timeout; returns `Boolean` |
| `tryAcquire(permits)` | Non-blocking acquire; returns `Boolean` |
| `reserveSlot(permits)` | Preview wait duration without consuming permits |
| `getAvailableSlots()` | Current available permit count |
| `getMetrics()` | Metrics snapshot |
| `changeLimitForPeriod(limit)` | Hot-reload permit limit |
| `changeTimeoutDuration(timeout)` | Hot-reload wait timeout |

## Events

| Event | When |
|-------|------|
| `SuccessfulAcquire(permits)` | Permits acquired |
| `FailedAcquire(permits, waitDuration)` | Acquisition rejected; includes computed wait |
| `RateLimiterDisposed` | Instance disposed |

## Metrics snapshot

`RateLimiterMetrics` exposes:

- `availablePermissions`
- `limitForPeriod`
- `limitRefreshPeriod`
- `numberOfWaitingThreads`
- `numberOfSuccessfulAcquires`
- `numberOfFailedAcquires`
- `totalWaitTime`

## Registry

```kotlin
val registry = rateLimiterRegistry { }
val api = registry.create("api") {
    limitForPeriod = 100
}
```

## Metrics collector

```kotlin
import com.davils.resilience.metrics.ratelimiter.metrics

val snapshot = limiter.metrics().refresh()
```

## Ktor plugin

```kotlin
install(RateLimiterPlugin) {
    rateLimiter = fixedRateLimiter(limit = 100, period = 1.seconds) {
        strategy = RateLimiterStrategy.FAIL_FAST
    }
}
```

Returns HTTP 429 with `Retry-After` when the limit is exceeded.
