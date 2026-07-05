# Time Limiter

The **davilsx-resilience-timelimiter** module provides execution time limiting for Kotlin Multiplatform applications. It integrates with the shared resilience component model (`ResilienceComponent`, event bus, registry) and supports hard and soft timeout strategies.

## Features

| Feature | Description |
|---------|-------------|
| **Hard timeout** | Cancels the guarded block when the timeout expires |
| **Soft timeout** | Runs work in a detached coroutine and stops waiting on timeout |
| **Fallback** | Optional substitute value when a timeout occurs |
| **Runtime config** | Change timeout without recreating the instance |
| **Metrics** | Snapshot API and metrics collector integration |
| **Events** | Observable timeout events |
| **Registry** | Named time limiter instances with shared defaults |
| **Convenience factories** | `hardTimeLimiter`, `softTimeLimiter` |

## Installation

### Composite build (this repository)

```kotlin
dependencies {
    implementation(project(":davilsx-resilience-timelimiter"))
}
```

### Gradle (Kotlin Multiplatform)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.davils:davilsx-resilience-timelimiter:<version>")
        }
    }
}
```

## Quick start

```kotlin
import com.davils.resilience.timelimiter.hardTimeLimiter
import kotlin.time.Duration.Companion.seconds

val limiter = hardTimeLimiter(timeout = 2.seconds)

limiter.execute {
    callRemoteService()
}
```

See [Configuration](Time-Limiter-Configuration.md), [API reference](Time-Limiter-API.md), and [Examples](Time-Limiter-Examples.md).

## Related modules

| Module | Purpose |
|--------|---------|
| `davilsx-resilience-metrics` | `TimeLimiter.metrics` collector |
| `davilsx-resilience-ktor` | HTTP 504 `TimeLimiterPlugin` |
| `davilsx-resilience-example:example-timelimiter` | Runnable demo |
