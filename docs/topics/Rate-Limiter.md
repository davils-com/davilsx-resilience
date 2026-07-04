# Rate Limiter

The **davilsx-resilience-ratelimiter** module provides thread-safe request rate limiting for Kotlin Multiplatform applications. It integrates with the shared resilience component model (`ResilienceComponent`, event bus, registry) and supports fixed and sliding window algorithms.

## Features

| Feature | Description |
|---------|-------------|
| **Fixed window** | Resilience4j-style periodic refresh with cycle-based permit accounting |
| **Sliding window** | Rolling time window for smoother rate enforcement |
| **Acquire strategies** | `FAIL_FAST`, `WAIT`, and `BLOCKING` behavior |
| **Multi-permit** | Acquire multiple permits in a single operation |
| **Runtime config** | Change limits and timeouts without recreating the instance |
| **Metrics** | Snapshot API and metrics collector integration |
| **Events** | Observable successful/failed acquisitions |
| **Registry** | Named rate limiter instances with shared defaults |
| **Convenience factories** | `fixedRateLimiter`, `slidingWindowRateLimiter` |

## Installation

### Composite build (this repository)

```kotlin
dependencies {
    implementation(project(":davilsx-resilience-ratelimiter"))
}
```

### Gradle (Kotlin Multiplatform)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.davils:davilsx-resilience-ratelimiter:<version>")
        }
    }
}
```

## Quick start

```kotlin
import com.davils.resilience.ratelimiter.fixedRateLimiter
import kotlin.time.Duration.Companion.milliseconds

val limiter = fixedRateLimiter(limit = 50, period = 500.milliseconds)

limiter.execute {
    callRemoteService()
}
```

See [Configuration](Rate-Limiter-Configuration.md), [API reference](Rate-Limiter-API.md), and [Examples](Rate-Limiter-Examples.md).

## Related modules

| Module | Purpose |
|--------|---------|
| `davilsx-resilience-metrics` | `RateLimiter.metrics` collector |
| `davilsx-resilience-ktor` | HTTP 429 `RateLimiterPlugin` |
| `davilsx-resilience-example:example-ratelimiter` | Runnable demo |
