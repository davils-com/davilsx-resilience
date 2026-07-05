# Rate Limiter — Configuration

## DSL builder

```kotlin
val limiter = rateLimiter {
    limitForPeriod = 50
    limitRefreshPeriod = 500.milliseconds
    timeoutDuration = 5.seconds
    strategy = RateLimiterStrategy.WAIT
    windowType = RateLimiterWindowType.FIXED
}
```

## Settings

| Property | Default | Description |
|----------|---------|-------------|
| `limitForPeriod` | `50` | Maximum permits per refresh period |
| `limitRefreshPeriod` | `500.milliseconds` | Duration of one refresh/window period |
| `timeoutDuration` | `5.seconds` | Maximum wait time before rejection (`WAIT` strategy) |
| `strategy` | `WAIT` | Acquire behavior when permits are unavailable |
| `windowType` | `FIXED` | `FIXED` or `SLIDING` algorithm |

## Strategies

| Strategy | Behavior |
|----------|----------|
| `FAIL_FAST` | Reject immediately when waiting would be required |
| `WAIT` | Wait up to `timeoutDuration`, then reject |
| `BLOCKING` | Wait indefinitely until a permit becomes available |

## Convenience factories

```kotlin
fixedRateLimiter(limit = 100, period = 1.seconds)
slidingWindowRateLimiter(limit = 100, period = 1.seconds)
```

Both accept an optional trailing `builder` block for further customization.

## Validation

- `limitForPeriod` must be at least `1`
- `limitRefreshPeriod` must be positive
- `timeoutDuration` must be non-negative

## Runtime changes

```kotlin
limiter.changeLimitForPeriod(100)
limiter.changeTimeoutDuration(10.seconds)
```
