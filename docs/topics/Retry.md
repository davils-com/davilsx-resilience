# Retry

The **davilsx-resilience-retry** module provides configurable retry policies for Kotlin Multiplatform applications. It integrates with the shared resilience component model (`ResilienceComponent`, event bus, registry) and supports throwable- and result-based retry conditions with pluggable backoff strategies.

## Features

| Feature | Description |
|---------|-------------|
| **Throwable predicates** | Retry on specific exception types, with optional cause-chain inspection |
| **Result predicates** | Retry when a successful return value matches a condition (e.g. HTTP 5xx) |
| **Composite predicates** | Combine policies with `anyOf` / `allOf` |
| **Backoff strategies** | Constant, exponential, and jitter (proportional, full, equal, decorrelated) |
| **Exhaustion modes** | Throw `MaxRetriesExceededException` or return the last rejected result |
| **Metrics** | Snapshot API and metrics collector integration |
| **Events** | Observable attempt lifecycle (started, failed, backoff, succeeded, cancelled) |
| **Registry** | Named retry policies with shared defaults |
| **Convenience factories** | `fixedDelayRetry`, `exponentialRetry` |

## Installation

### Composite build (this repository)

```kotlin
dependencies {
    implementation(project(":davilsx-resilience-retry"))
}
```

### Gradle (Kotlin Multiplatform)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.davils:davilsx-resilience-retry:<version>")
        }
    }
}
```

## Quick start

```kotlin
import com.davils.resilience.retry.exponentialRetry

val retry = exponentialRetry(maxAttempts = 3)

retry.execute {
    callRemoteService()
}
```

See [Configuration](Retry-Configuration.md), [API reference](Retry-API.md), and [Examples](Retry-Examples.md).

## Related modules

| Module | Purpose |
|--------|---------|
| `davilsx-resilience-metrics` | `Retry.metrics` collector |
| `davilsx-resilience-example:example-retry` | Runnable demo |
