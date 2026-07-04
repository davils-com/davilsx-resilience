# Cache configuration

Caches can be created with **convenience factories** (recommended for common setups) or the generic **`cache { }` DSL** (full control). Both paths use the same `CacheBuilder` and validate at creation time; invalid configuration throws `DslVerificationException`.

## Convenience factories

Preset functions live in `com.davils.resilience.cache` and delegate to `cache { }`. Each applies defaults first, then runs your optional builder:

```kotlin
import com.davils.resilience.cache.lruCache
import com.davils.resilience.cache.writeThroughCache
import com.davils.resilience.cache.store.inMemoryCacheStore
import kotlin.time.Duration.Companion.minutes

// Bounded LRU cache with TTL
val hot = lruCache<String, User>(maxSize = 500) {
    expireAfterWrite(10.minutes)
}

// Backing store with synchronous writes
val backed = writeThroughCache(inMemoryCacheStore()) {
    maxSize(1000)
}
```

| Factory | Applies |
|---------|---------|
| `lruCache(maxSize, builder)` | `maxSize` + `EvictionStrategyType.LRU` |
| `lfuCache(maxSize, builder)` | `maxSize` + `EvictionStrategyType.LFU` |
| `fifoCache(maxSize, builder)` | `maxSize` + `EvictionStrategyType.FIFO` |
| `inMemoryCache(builder)` | No store (equivalent to `cache(builder)`) |
| `expiringCache(expireAfterWrite, expireAfterAccess, cleanupInterval, builder)` | TTL settings |
| `writeThroughCache(store, builder)` | `store` + `WriteStrategy.WRITE_THROUGH` |
| `writeBackCache(store, builder)` | `store` + `WriteStrategy.WRITE_BACK` |

The builder block can add or override anything — store, events, write-back tuning, or even a different eviction strategy.

## Generic DSL

Use `cache { }` when no preset matches or you want a single explicit configuration block:

```kotlin
import com.davils.resilience.cache.*
import com.davils.resilience.cache.store.inMemoryCacheStore
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val cache = cache<String, User> {
    maxSize(1000)
    evictionStrategy(EvictionStrategyType.LRU)

    expireAfterWrite(10.minutes)
    expireAfterAccess(5.minutes)
    cleanupInterval(1.minutes)

    store(inMemoryCacheStore())
    writeStrategy(WriteStrategy.WRITE_THROUGH)

    event {
        // optional event bus configuration (see API reference)
    }
}
```

## Builder properties

| Property | Default | Description |
|----------|---------|-------------|
| `maxSize` | `1000` | Maximum number of entries before eviction runs |
| `evictionStrategy` | `EvictionStrategy.Lru` | Policy used when `maxSize` is exceeded |
| `expireAfterWrite` | `Duration.ZERO` | Max age since creation; `ZERO` disables |
| `expireAfterAccess` | `Duration.ZERO` | Max idle time since last access; `ZERO` disables |
| `cleanupInterval` | `Duration.ZERO` | Background expiry sweep interval; `ZERO` disables |
| `store` | `null` | Optional `CacheStore` for read/write-through or write-back |
| `writeStrategy` | `WRITE_THROUGH` | How puts are persisted when a store is set |

Duration values accept either `Duration` or milliseconds as `Long`:

```kotlin
expireAfterWrite(5000L)          // 5000 ms
expireAfterWrite(5.seconds)      // preferred
```

## Eviction strategies

Configure via enum or pass a custom `EvictionStrategy`:

```kotlin
evictionStrategy(EvictionStrategyType.LRU)
evictionStrategy(EvictionStrategyType.LFU)
evictionStrategy(EvictionStrategyType.FIFO)

// custom strategy
evictionStrategy(myCustomStrategy)
```

| Strategy | Evicts |
|----------|--------|
| **LRU** | Entry accessed least recently |
| **LFU** | Entry with fewest accesses (FIFO tie-break by insertion order) |
| **FIFO** | Earliest inserted entry |

Custom strategies implement `EvictionStrategy.selectVictim(entries)` and must be stateless; eviction metadata lives in `CacheEntry`.

## TTL behavior

Two independent TTL knobs exist:

- **`expireAfterWrite`** — measured from entry creation
- **`expireAfterAccess`** — measured from last read; refreshed on `get`, `get(key, loader)`, and `contains`

Expiration is **lazy**: expired entries are removed on the next access attempt. When `cleanupInterval > 0`, a background coroutine also removes expired entries without requiring access.

Set either duration to `Duration.ZERO` to disable that rule.

## Write strategies

Requires a configured `store`.

| Strategy | Behavior |
|----------|----------|
| `WRITE_THROUGH` | Every `put` calls `store.store` synchronously; `remove`/`clear` mirror to the store |
| `WRITE_BACK` | Puts buffer in a dirty map; flushed periodically, when batch size is reached, manually via `flush()`, or on `dispose` (if enabled) |

### Write-back block

When using `WRITE_BACK`, configure flushing with the nested `writeBack { }` block (works with `cache { }` or `writeBackCache(store) { }`):

```kotlin
writeBackCache(myStore) {
    maxSize(500)
    writeBack {
        flushInterval(5.seconds)   // periodic flush (default: 5 s)
        batchSize(100)             // flush when dirty count reaches this (default: 100)
        flushOnDispose(true)       // flush remaining dirty entries on dispose (default: true)
    }
}
```

| Write-back property | Default | Validation |
|---------------------|---------|------------|
| `flushInterval` | `5.seconds` | Must be positive |
| `batchSize` | `100` | Must be ≥ 1 |
| `flushOnDispose` | `true` | — |

## Event configuration

Use the shared `event { }` block from `ResilienceComponentBuilder`:

```kotlin
event {
    scope = CoroutineScope(Dispatchers.Default)
    replay = 0
    overflowStrategy = BufferOverflow.DROP_OLDEST
    extraBufferCapacity = 128
    onError = { throwable -> logger.error(throwable) { "cache event error" } }
}
```

Events drive observability and metrics integrations. See [API reference — Events](Cache-API.md#events).

## Validation rules

Configuration fails at `cache { }` creation time when:

| Rule | Field |
|------|-------|
| `maxSize < 1` | `maxSize` |
| Negative TTL or cleanup interval | `expireAfterWrite`, `expireAfterAccess`, `cleanupInterval` |
| `WRITE_BACK` without a store | `store` |
| Invalid write-back settings | `flushInterval`, `batchSize` |

Pure in-memory caches (no store) need no write strategy configuration.

## Registry defaults

Registries apply a shared default configuration to caches created without an explicit builder:

```kotlin
val registry = cacheRegistry<String, User> { }

registry.default {
    maxSize(500)
    expireAfterAccess(30.minutes)
}

val sessions = registry.getOrCreate("sessions")
val users = registry.create("users") { maxSize(1000) }  // overrides default maxSize
```

See [API reference — Registry](Cache-API.md#registry).
