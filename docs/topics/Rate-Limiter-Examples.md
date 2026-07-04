# Rate Limiter — Examples

## Basic protection

```kotlin
val limiter = fixedRateLimiter(limit = 10, period = 1.seconds)

suspend fun callApi(): String = limiter.execute {
    httpClient.get("https://api.example.com/data")
}
```

## Fail-fast for APIs

```kotlin
val limiter = fixedRateLimiter(limit = 100, period = 1.seconds) {
    strategy = RateLimiterStrategy.FAIL_FAST
}

if (!limiter.tryAcquire()) {
    return ServiceUnavailable
}
```

## Sliding window for smoother limits

```kotlin
val limiter = slidingWindowRateLimiter(limit = 50, period = 500.milliseconds) {
    timeoutDuration = 2.seconds
}
```

## Monitoring with events

```kotlin
limiter.subscribe<RateLimiterEvent.FailedAcquire> { event ->
    logger.warn("Rate limit hit, wait=${event.waitDuration}")
}
```

## Registry with shared defaults

```kotlin
val registry = rateLimiterRegistry {
    default {
        limitForPeriod = 50
        limitRefreshPeriod = 500.milliseconds
    }
}

val userApi = registry.create("user-api")
val adminApi = registry.create("admin-api") {
    limitForPeriod = 10
}
```

## Ktor server

```kotlin
fun Application.module() {
    install(RateLimiterPlugin) {
        rateLimiter = fixedRateLimiter(limit = 60, period = 1.minutes) {
            strategy = RateLimiterStrategy.FAIL_FAST
        }
    }
    routing {
        get("/api") { call.respond("ok") }
    }
}
```

See `davilsx-resilience-example/example-ratelimiter` for a runnable JVM demo.
