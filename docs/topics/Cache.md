# Cache

The **davilsx-resilience-cache** module provides a thread-safe, DSL-configured cache for Kotlin Multiplatform applications. It integrates with the shared resilience component model (`ResilienceComponent`, event bus, registry) and supports eviction policies, TTL expiration, read-through loading, and write-through or write-back persistence.

## Features

| Feature | Description |
|---------|-------------|
| **Thread safety** | Internal storage uses kore `ConcurrentHashMap` |
| **Eviction** | LRU, LFU, FIFO (pluggable via `EvictionStrategy`) |
| **TTL** | `expireAfterWrite` and `expireAfterAccess` with lazy expiry on access |
| **Active cleanup** | Optional background job removes expired entries |
| **Read-through** | Loads missing keys from a `CacheStore` on cache miss |
| **Write-through** | Persists writes synchronously to the backing store |
| **Write-back** | Buffers writes and flushes on interval, batch size, or dispose |
| **Events** | Observable hit/miss/eviction/expiration/load/write events |
| **Registry** | Named cache instances with shared defaults |
| **Convenience factories** | `lruCache`, `lfuCache`, `fifoCache`, `expiringCache`, `writeThroughCache`, and more |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Cache<K, V>                        │
│  ┌─────────────────┐    ┌─────────────────────────────┐ │
│  │ ConcurrentHashMap│    │ Maintenance jobs            │ │
│  │ (hot entries)   │    │ • TTL cleanup               │ │
│  └────────┬────────┘    │ • Write-back flush          │ │
│           │             └─────────────────────────────┘ │
│           │ read miss / write                           │
│           ▼                                             │
│  ┌─────────────────┐                                    │
│  │   CacheStore    │  ← optional backing layer          │
│  └─────────────────┘                                    │
└─────────────────────────────────────────────────────────┘
```

Without a `CacheStore`, the cache is a pure in-memory component. When a store is configured, misses trigger read-through loads and puts follow the selected write strategy.

## Installation

### Gradle (Kotlin Multiplatform)

Add the dependency to your `commonMain` (or platform-specific) source set:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.davils:davilsx-resilience-cache:<version>")
        }
    }
}
```

Replace `<version>` with the release that matches your other `davilsx-resilience-*` artifacts.

### Gradle (JVM only)

If you consume the JVM artifact directly:

```kotlin
dependencies {
    implementation("com.davils:davilsx-resilience-cache-jvm:<version>")
}
```

### Composite build (this repository)

When working inside the monorepo:

```kotlin
dependencies {
    implementation(project(":davilsx-resilience-cache"))
}
```

See also the runnable examples in `davilsx-resilience-example/example-bulkhead`.

### Transitive dependencies

The cache module depends on:

- **davilsx-resilience-common** — component base class, event configuration, registry
- **davilsx-kore** — `ConcurrentHashMap`, DSL validation, event bus

Coroutines are required at runtime because all cache operations are `suspend` functions.

### Supported targets

The module is published for all KMP targets configured in the project (JVM, JS, Wasm, Android, iOS, Linux, macOS, and others). Use the standard KMP dependency declaration; Gradle resolves the correct platform artifact automatically.

## Quick start

```kotlin
import com.davils.resilience.cache.lruCache
import kotlin.time.Duration.Companion.minutes

suspend fun example() {
    val cache = lruCache<String, User>(maxSize = 500) {
        expireAfterWrite(10.minutes)
    }

    cache.put("user:42", user)
    val cached = cache.get("user:42")

    cache.dispose()
}
```

For the generic DSL when you need full control, use `cache { }`:

```kotlin
import com.davils.resilience.cache.cache

val cache = cache<String, User> {
    maxSize(500)
    expireAfterWrite(10.minutes)
}
```

For load-on-miss without a backing store:

```kotlin
val user = cache.get("user:42") { key ->
    userRepository.findById(key) ?: error("not found")
}
```

## Documentation map

| Topic | Contents |
|-------|----------|
| [Configuration](Cache-Configuration.md) | Convenience factories and DSL builder properties, defaults, validation |
| [API reference](Cache-API.md) | `Cache`, `CacheStore`, registry, events, lifecycle |
| [Examples](Cache-Examples.md) | Copy-paste recipes for common scenarios |

## Package overview

| Package / type | Role |
|----------------|------|
| `com.davils.resilience.cache` | Core types: `Cache`, `cache()`, convenience factories, `CacheBuilder`, events |
| `com.davils.resilience.cache.store` | Built-in `CacheStore` implementations (`InMemoryCacheStore`, `DelegatingCacheStore`) |

Primary entry points:

- `cache { }` — create a fully custom cache instance
- **Convenience factories** — preset common configurations (see below)
- `cacheRegistry { }` — manage named caches
- `inMemoryCacheStore()` / `delegatingCacheStore()` — ready-made backing stores

## Convenience factories

Preset functions apply common configuration before an optional builder block. The builder runs last and can override any preset value.

| Factory | Preset |
|---------|--------|
| `lruCache(maxSize, builder)` | Bounded cache with LRU eviction |
| `lfuCache(maxSize, builder)` | Bounded cache with LFU eviction |
| `fifoCache(maxSize, builder)` | Bounded cache with FIFO eviction |
| `inMemoryCache(builder)` | Pure in-memory cache (alias for `cache { }`) |
| `expiringCache(expireAfterWrite, …, builder)` | TTL with optional access expiry and cleanup |
| `writeThroughCache(store, builder)` | Backing store with synchronous writes |
| `writeBackCache(store, builder)` | Backing store with buffered writes |

Combine presets freely via the builder:

```kotlin
import com.davils.resilience.cache.lruCache
import com.davils.resilience.cache.WriteStrategy
import kotlin.time.Duration.Companion.minutes

val users = lruCache<UserId, User>(maxSize = 500) {
    expireAfterWrite(15.minutes)
    store(userStore)
    writeStrategy(WriteStrategy.WRITE_THROUGH)
}
```

### Choosing a factory

| You need… | Start with |
|-----------|------------|
| Bounded cache with explicit eviction policy | `lruCache`, `lfuCache`, or `fifoCache` |
| Pure in-memory, no backing store | `inMemoryCache` |
| TTL / session-style expiry | `expiringCache` |
| Read-through + synchronous persistence | `writeThroughCache(store)` |
| Buffered writes to a backing store | `writeBackCache(store)` |
| Full control over every option | `cache { }` |

Presets are applied first; the optional `builder` block runs last and can override any value (for example, `lfuCache(2) { evictionStrategy(LRU) }`).

See [Configuration — Convenience factories](Cache-Configuration.md#convenience-factories) and [Examples](Cache-Examples.md) for copy-paste recipes.
