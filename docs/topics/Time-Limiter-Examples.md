# Time Limiter — Examples

## Basic protection

```kotlin
val limiter = hardTimeLimiter(timeout = 2.seconds)

suspend fun callApi(): String = limiter.execute {
    httpClient.get("https://api.example.com/data")
}
```

## Soft timeout for fire-and-forget work

```kotlin
val limiter = softTimeLimiter(timeout = 500.milliseconds) {
    cancelOnTimeout = false
}

limiter.execute {
    longRunningCleanup()
}
```

## Fallback on timeout

```kotlin
val limiter = hardTimeLimiter(timeout = 1.seconds) {
    fallback { "cached-default" }
}

val result = limiter.execute {
    slowRemoteCall()
}
```

## Monitoring with events

```kotlin
limiter.subscribe<TimeLimiterEvent.TimeoutExceeded> { event ->
    logger.warn("Timeout after ${event.timeoutMs}ms")
}
```

## Registry with shared defaults

```kotlin
val registry = timeLimiterRegistry {
    default {
        timeout = 2.seconds
        strategy = TimeoutStrategy.HARD
    }
}

val userApi = registry.create("user-api")
val batchJob = registry.create("batch-job") {
    timeout = 30.seconds
    strategy = TimeoutStrategy.SOFT
}
```

## Ktor server

```kotlin
fun Application.module() {
    install(TimeLimiterPlugin) {
        timeLimiter = hardTimeLimiter(timeout = 3.seconds)
    }
    routing {
        get("/api") { call.respond("ok") }
    }
}
```

See `davilsx-resilience-example/example-timelimiter` for a runnable JVM demo.
