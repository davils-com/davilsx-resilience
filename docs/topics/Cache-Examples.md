# Cache examples

Practical snippets for common caching scenarios. Full runnable code lives in `davilsx-resilience-example/example-bulkhead/src/main/kotlin/com/davils/example/cache/Main.kt`.

## Factory cheat sheet

| Scenario | Factory |
|----------|---------|
| Simple in-memory | `inMemoryCache { }` |
| Bounded LRU / LFU / FIFO | `lruCache(maxSize)`, `lfuCache(maxSize)`, `fifoCache(maxSize)` |
| TTL expiry | `expiringCache(expireAfterWrite, …)` |
| Store + sync writes | `writeThroughCache(store) { }` |
| Store + buffered writes | `writeBackCache(store) { writeBack { … } }` |
| Everything custom | `cache { }` |

---

## Basic in-memory cache

```kotlin
import com.davils.resilience.cache.inMemoryCache

suspend fun basic() {
    val cache = inMemoryCache<String, String> { maxSize(100) }

    cache.put("key", "value")
    println(cache.get("key"))       // value
    println(cache.contains("key"))  // true
    println(cache.size())           // 1

    cache.remove("key")
    cache.dispose()
}
```

## Load on miss with a loader function

Use when there is no backing store but you want compute-on-miss semantics:

```kotlin
import com.davils.resilience.cache.inMemoryCache

val cache = inMemoryCache<Int, Int> { maxSize(64) }

val squared = cache.get(9) { key ->
    expensiveComputation(key)  // called once
}

val cached = cache.get(9) { key ->
    error("not called on hit")
}
```

## LRU eviction

```kotlin
import com.davils.resilience.cache.lruCache

val cache = lruCache<String, String>(maxSize = 2)

cache.put("a", "1")
cache.put("b", "2")
cache.get("a")        // refreshes "a"
cache.put("c", "3")   // evicts "b" (least recently used)

cache.contains("b")   // false
cache.contains("a")   // true
```

## LFU eviction

Evicts the entry with the fewest accesses; ties break by insertion order.

```kotlin
import com.davils.resilience.cache.lfuCache

val cache = lfuCache<String, String>(maxSize = 2)

cache.put("a", "1")
cache.put("b", "2")
cache.get("a")
cache.get("a")        // "a" accessed twice
cache.put("c", "3")   // evicts "b" (lowest frequency)

cache.contains("a")   // true
cache.contains("b")   // false
```

## FIFO eviction

Evicts the earliest inserted entry regardless of later access.

```kotlin
import com.davils.resilience.cache.fifoCache

val cache = fifoCache<String, String>(maxSize = 2)

cache.put("a", "1")
cache.put("b", "2")
cache.get("a")        // access does not protect "a" under FIFO
cache.put("c", "3")   // evicts "a" (first inserted)

cache.contains("a")   // false
cache.contains("b")   // true
```

## TTL expiration

```kotlin
import com.davils.resilience.cache.expiringCache
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

val cache = expiringCache<String, String>(
    expireAfterWrite = 200.milliseconds,
    cleanupInterval = 100.milliseconds,
)

cache.put("token", "abc123")
cache.get("token")    // abc123

delay(300)
cache.get("token")    // null — expired lazily on access
```

Access-based TTL refreshes on every read:

```kotlin
import com.davils.resilience.cache.expiringCache
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

val cache = expiringCache<String, String>(
    expireAfterWrite = Duration.ZERO,
    expireAfterAccess = 5.minutes,
)
```

## Read-through and write-through

Combine a hot cache with a slower store. On miss, the store is queried automatically; puts persist immediately.

```kotlin
import com.davils.resilience.cache.writeThroughCache
import com.davils.resilience.cache.store.inMemoryCacheStore

val store = inMemoryCacheStore(initial = mapOf("u1" to "Ada Lovelace"))

val cache = writeThroughCache(store)

// Read-through on miss
val user = cache.get("u1")

// Write-through on put
cache.put("u2", "Grace Hopper")
println(store.snapshot())  // u1 and u2 present in backing store
```

Combine with bounded eviction:

```kotlin
import kotlin.time.Duration.Companion.minutes

val cache = writeThroughCache(store) {
    maxSize(500)
    expireAfterWrite(15.minutes)
}
```

## Write-back buffering

Reduce write load by batching persistence:

```kotlin
import com.davils.resilience.cache.writeBackCache
import com.davils.resilience.cache.store.inMemoryCacheStore
import kotlin.time.Duration.Companion.seconds

val store = inMemoryCacheStore()

val cache = writeBackCache(store) {
    writeBack {
        flushInterval(2.seconds)
        batchSize(10)
        flushOnDispose(true)
    }
}

cache.put("a", "1")
cache.put("b", "2")
// store may still be empty until flush

cache.flush()              // manual flush
cache.dispose()            // flushes if flushOnDispose = true
```

## Custom store via delegates

Connect an existing repository without a dedicated class:

```kotlin
import com.davils.resilience.cache.writeThroughCache
import com.davils.resilience.cache.store.delegatingCacheStore
import kotlinx.coroutines.delay

val store = delegatingCacheStore<String, User>(
    load = { id ->
        delay(50)  // simulate latency
        database.findUser(id)
    },
    store = { id, user ->
        delay(50)
        database.saveUser(id, user)
    },
    remove = { id ->
        database.deleteUser(id)
    },
)

val cache = writeThroughCache(store)
```

## Event observability

```kotlin
import com.davils.resilience.cache.CacheEvent
import com.davils.resilience.cache.fifoCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow

val cache = fifoCache<String, String>(maxSize = 1) {
    event {
        scope = CoroutineScope(Dispatchers.Default)
        replay = 0
        overflowStrategy = BufferOverflow.DROP_OLDEST
        extraBufferCapacity = 128
    }
}

cache.subscribe(CacheEvent::class) { event ->
    println(event::class.simpleName)
}

cache.put("key", "value")  // CachePut
cache.get("key")           // CacheHit
cache.get("missing")       // CacheMiss
```

## Named caches with a registry

```kotlin
import com.davils.resilience.cache.cacheRegistry
import kotlin.time.Duration.Companion.seconds

val registry = cacheRegistry<String, User> { }

registry.default {
    maxSize(500)
    expireAfterAccess(30.seconds)
}

// Created once, reused on subsequent calls
val sessions = registry.getOrCreate("sessions")
val same = registry.getOrCreate("sessions")  // identical instance

sessions.put("session-1", "user-42")

registry.dispose()  // disposes all registered caches
```

## Combining factories

Factories compose through the builder block:

```kotlin
import com.davils.resilience.cache.lfuCache
import com.davils.resilience.cache.WriteStrategy
import kotlin.time.Duration.Companion.seconds

val metrics = lfuCache<String, Counter>(maxSize = 5_000) {
    store(metricsStore)
    writeStrategy(WriteStrategy.WRITE_BACK)
    writeBack {
        flushInterval(1.seconds)
        batchSize(50)
    }
}
```

Override a preset when needed:

```kotlin
import com.davils.resilience.cache.lfuCache
import com.davils.resilience.cache.EvictionStrategyType

// Starts as LFU, builder switches to LRU
val cache = lfuCache<String, String>(maxSize = 2) {
    evictionStrategy(EvictionStrategyType.LRU)
}
```

## Recommended patterns

### Session or token cache

```kotlin
import com.davils.resilience.cache.lruCache
import kotlin.time.Duration.Companion.minutes

val sessions = lruCache<String, Session>(maxSize = 10_000) {
    expireAfterAccess(30.minutes)
    cleanupInterval(1.minutes)
}
```

### Database-backed entity cache

```kotlin
import com.davils.resilience.cache.lruCache
import com.davils.resilience.cache.WriteStrategy
import com.davils.resilience.cache.writeThroughCache
import kotlin.time.Duration.Companion.minutes

// Option A: preset + builder
val users = lruCache<UserId, User>(maxSize = 1_000) {
    expireAfterWrite(15.minutes)
    store(userRepositoryStore)
    writeStrategy(WriteStrategy.WRITE_THROUGH)
}

// Option B: write-through factory + builder
val usersAlt = writeThroughCache(userRepositoryStore) {
    maxSize(1_000)
    expireAfterWrite(15.minutes)
}
```

### High-write throughput with write-back

```kotlin
import com.davils.resilience.cache.lfuCache
import com.davils.resilience.cache.WriteStrategy
import kotlin.time.Duration.Companion.seconds

val metrics = lfuCache<String, Counter>(maxSize = 5_000) {
    store(metricsStore)
    writeStrategy(WriteStrategy.WRITE_BACK)
    writeBack {
        flushInterval(1.seconds)
        batchSize(50)
    }
}
```

Always call `dispose()` when shutting down the application scope to cancel background jobs and flush pending write-back entries.
