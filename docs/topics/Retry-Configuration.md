# Retry — Configuration

## DSL builder

```kotlin
val policy = retry {
    maxAttempts = 3
    failAfterMaxRetries = true
    onResultExhaustion = OnResultExhaustion.THROW
    backoffStrategy = exponentialBackoff {
        initialDelay = 500.milliseconds
        multiplier = 2.0
        maxDelay = 30.seconds
    }
    predicate = throwablePredicate {
        throwable(IOException::class)
        ignore(SocketTimeoutException::class)
    }
}
```

## Settings

| Property | Default | Description |
|----------|---------|-------------|
| `maxAttempts` | `3` | Total attempts including the initial call |
| `failAfterMaxRetries` | `true` | Stop after `maxAttempts`; when `false`, retry indefinitely while the predicate matches |
| `onResultExhaustion` | `THROW` | Behavior when a result predicate still requests retry at `maxAttempts` |
| `backoffStrategy` | constant 1000ms | Delay between attempts |
| `predicate` | retry all throwables | When to schedule another attempt |

## Predicates

### Throwable

```kotlin
throwablePredicate {
    throwable(IOException::class)
    retryOnAll()
    causeChain()
}
```

### Result

```kotlin
resultPredicate<HttpResponse> {
    retryIf { it.status.value in 500..599 }
    retryOnNull()
}
```

### Composite

```kotlin
predicate = anyOf(
    throwablePredicate { throwable(IOException::class) },
    resultPredicate { retryIf { it == "RETRY" } },
)
```

## Backoff strategies

| Factory | Description |
|---------|-------------|
| `constantBackoff { delay(1.seconds) }` | Fixed delay |
| `exponentialBackoff { ... }` | Exponential growth capped by `maxDelay` |
| `jitterBackoff(base) { factor(0.5) }` | Randomized delay around a base strategy |

Jitter modes: `PROPORTIONAL`, `FULL`, `EQUAL`, `DECORRELATED`.

## Convenience factories

```kotlin
fixedDelayRetry(maxAttempts = 3, delay = 1.seconds)
exponentialRetry(maxAttempts = 5, initialDelay = 500.milliseconds)
```

Both accept an optional trailing `builder` block for further customization.

## Validation

- `maxAttempts` must be at least `1`
- Backoff builder properties validate on assignment (negative delays, invalid jitter factor, etc.)
