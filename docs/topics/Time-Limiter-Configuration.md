# Time Limiter — Configuration

## DSL builder

```kotlin
val limiter = timeLimiter {
    timeout = 2.seconds
    strategy = TimeoutStrategy.HARD
    cancelOnTimeout = true
    fallback<String> { "default" }
}
```

## Settings

| Property | Default | Description |
|----------|---------|-------------|
| `timeout` | `1.seconds` | Maximum execution duration. Set to `Duration.ZERO` to disable limiting |
| `strategy` | `HARD` | `HARD` or `SOFT` timeout behavior |
| `cancelOnTimeout` | `true` | Whether soft timeouts cancel background work |
| `fallback` | `null` | Optional handler invoked on timeout |

## Strategies

| Strategy | Behavior |
|----------|----------|
| `HARD` | Wraps the block in `withTimeout`; cancels the coroutine on expiry |
| `SOFT` | Runs the block in a detached coroutine; stops waiting on expiry |

## Convenience factories

```kotlin
hardTimeLimiter(timeout = 1.seconds)
softTimeLimiter(timeout = 1.seconds) {
    cancelOnTimeout = false
}
```

Both accept an optional trailing `builder` block for further customization.

## Validation

- `timeout` must be non-negative

## Runtime changes

```kotlin
limiter.changeTimeout(5.seconds)
```
