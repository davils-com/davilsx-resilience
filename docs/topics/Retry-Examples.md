# Retry — Examples

## Basic throwable retry

```kotlin
val policy = fixedDelayRetry(maxAttempts = 3, delay = 200.milliseconds)

suspend fun fetchData(): String = policy.execute {
    httpClient.get("https://api.example.com/data")
}
```

## Exponential backoff

```kotlin
val policy = exponentialRetry(maxAttempts = 5) {
    predicate = throwablePredicate { throwable(IOException::class) }
}
```

## Result-based retry (HTTP-style)

```kotlin
data class ApiResponse(val status: Int, val body: String)

val policy = retry {
    maxAttempts = 4
    predicate = resultPredicate<ApiResponse> {
        retryIf { it.status in 500..599 }
    }
    backoffStrategy = exponentialBackoff { initialDelay(250L) }
}
```

## Composite: retry on I/O errors or 5xx results

```kotlin
val policy = retry {
    maxAttempts = 3
    predicate = anyOf(
        throwablePredicate { throwable(IOException::class) },
        resultPredicate<ApiResponse> { retryIf { it.status >= 500 } },
    )
}
```

## Return last rejected result

```kotlin
val policy = retry {
    maxAttempts = 3
    onResultExhaustion = OnResultExhaustion.RETURN_LAST
    predicate = resultPredicate<String> { retryIf { it.isBlank() } }
}
```

## Monitoring with events

```kotlin
policy.subscribe<RetryEvent.RetryAttemptFailed> { event ->
    logger.warn("Attempt ${event.attempt} failed", event.throwable)
}
```

## Registry with shared defaults

```kotlin
val registry = retryRegistry {
    default {
        maxAttempts = 3
        backoffStrategy = exponentialBackoff()
    }
}

val userApi = registry.create("user-api")
val paymentApi = registry.create("payment-api") {
    maxAttempts = 5
}
```

See `davilsx-resilience-example/example-retry` for a runnable JVM demo.
