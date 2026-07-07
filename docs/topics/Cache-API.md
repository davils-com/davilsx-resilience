# Cache API reference

All cache operations are **suspend functions** and must be called from a coroutine.

## Factory functions

### `cache(builder)`

```kotlin
public fun <K, V> cache(builder: CacheBuilder<K, V>.() -> Unit): Cache<K, V>
```

Creates and validates a cache instance.

### Convenience factories

Preset functions in `com.davils.resilience.cache` delegate to `cache { }`. Presets run first; the optional `builder` block runs last and can override any value.

| Function | Preset |
|----------|--------|
| `lruCache(maxSize = 1000, builder)` | Bounded LRU cache |
| `lfuCache(maxSize = 1000, builder)` | Bounded LFU cache |
| `fifoCache(maxSize = 1000, builder)` | Bounded FIFO cache |
| `inMemoryCache(builder)` | Pure in-memory (no store) |
| `expiringCache(expireAfterWrite, expireAfterAccess = ZERO, cleanupInterval = ZERO, builder)` | TTL configuration |
| `writeThroughCache(store, builder)` | Store + write-through |
| `writeBackCache(store, builder)` | Store + write-back |

Full signatures:

```kotlin
public fun <K, V> lruCache(maxSize: Int = 1000, builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
public fun <K, V> lfuCache(maxSize: Int = 1000, builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
public fun <K, V> fifoCache(maxSize: Int = 1000, builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
public fun <K, V> inMemoryCache(builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
public fun <K, V> expiringCache(
    expireAfterWrite: Duration,
    expireAfterAccess: Duration = Duration.ZERO,
    cleanupInterval: Duration = Duration.ZERO,
    builder: CacheBuilder<K, V>.() -> Unit = {},
): Cache<K, V>
public fun <K, V> writeThroughCache(store: CacheStore<K, V>, builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
public fun <K, V> writeBackCache(store: CacheStore<K, V>, builder: CacheBuilder<K, V>.() -> Unit = {}): Cache<K, V>
```

`writeBackCache` pairs with the nested `writeBack { }` builder block for flush interval, batch size, and dispose behavior. See [Configuration](Cache-Configuration.md#write-back-block).

### `cacheRegistry(builder)`

```kotlin
public fun <K, V> cacheRegistry(builder: ResilienceRegistryBuilder.() -> Unit): CacheRegistry<K, V>
```

Creates a registry for named `Cache<K, V>` instances.

### Store factories

```kotlin
// com.davils.resilience.cache.store
public fun <K, V> inMemoryCacheStore(initial: Map<K, V> = emptyMap()): InMemoryCacheStore<K, V>

public fun <K, V> delegatingCacheStore(
    load: suspend (K) -> V?,
    store: suspend (K, V) -> Unit,
    remove: suspend (K) -> Unit,
): DelegatingCacheStore<K, V>
```

## Cache operations

### Reads

| Method | Events | Store on miss | Notes |
|--------|--------|---------------|-------|
| `get(key)` | Hit / Miss | Read-through load | Returns `null` if absent |
| `getOrNull(key)` | None | No | Cache-only lookup |
| `get(key, loader)` | Hit / Miss | Read-through, then loader | Loader runs only if store returns null |
| `contains(key)` | None | No | Checks validity including TTL |

```kotlin
// Read-through: store checked before returning null
val user = cache.get("user:42")

// Loader fallback when store has no value
val user = cache.get("user:42") { key ->
    api.fetchUser(key)
}
```

On concurrent misses for the same key, loads are coalesced: one store load or loader runs; other waiters re-check the cache and receive the same result.

### Atomic compute

Per-key locking serializes concurrent compute calls for the same key. All methods emit hit/miss/put/remove events consistent with their outcome.

| Method | Behavior |
|--------|----------|
| `putIfAbsent(key, value)` | Stores only when absent; returns existing value or `null` if stored |
| `computeIfAbsent(key, mappingFunction)` | Maps only when absent in memory; **no** store read-through; coalesced per key |
| `compute(key, remappingFunction)` | Remaps `(key, current?) -> new?`; `null` removes a present entry |
| `replace(key, value)` | Replaces only when present; returns previous value or `null` |
| `replace(key, oldValue, newValue)` | Replaces only when current value matches; returns `true`/`false` |
| `getAndPut(key, value)` | Stores and returns previous value or `null` |

Use `get(key, loader)` when a miss should read through the backing [CacheStore] before invoking a loader. Use `computeIfAbsent` for in-memory-only population under contention.

### Writes and maintenance

| Method | Description |
|--------|-------------|
| `put(key, value)` | Inserts or replaces; may evict; persists per write strategy |
| `remove(key)` | Removes from cache; flushes buffered write-back value first; mirrors removal in write-through mode |
| `clear()` | Removes all entries; flushes write-back dirty entries first; mirrors removals in write-through mode |
| `flush()` | Flushes write-back dirty entries to the store (no-op for write-through) |

### Introspection

| Method | Returns |
|--------|---------|
| `size()` | Entry count (may include not-yet-cleaned expired entries) |
| `keys()` | Snapshot of all keys |
| `contains(key)` | Whether a valid, non-expired entry exists |

### Lifecycle

| Method | Description |
|--------|-------------|
| `dispose()` | Cancels maintenance jobs, optionally flushes write-back, clears entries, closes event bus |
| `isDisposed()` | Whether the cache has been disposed |

After disposal, operations throw `CancellationException`.

## CacheStore

Interface for the slower persistence layer behind the hot cache:

```kotlin
public interface CacheStore<K, V> {
    public suspend fun load(key: K): V?
    public suspend fun store(key: K, value: V)
    public suspend fun remove(key: K)
}
```

Implement this interface—or use the built-in adapters—to connect databases, HTTP APIs, or repositories.

### Built-in stores

#### `InMemoryCacheStore`

Thread-safe map backed by kore `ConcurrentHashMap`. Useful for tests, local simulation, and prototyping.

```kotlin
import com.davils.resilience.cache.store.inMemoryCacheStore

val store = inMemoryCacheStore(initial = mapOf("seed" to "value"))
val snapshot = store.snapshot()  // debugging / verification
```

#### `DelegatingCacheStore`

Adapts suspend lambdas without writing a class:

```kotlin
import com.davils.resilience.cache.store.delegatingCacheStore

val store = delegatingCacheStore(
    load = { key -> repository.find(key) },
    store = { key, value -> repository.save(key, value) },
    remove = { key -> repository.delete(key) },
)
```

Load failures during read-through emit `CacheLoadFailure` and return `null` (they do not throw). Write-through `put` propagates store exceptions after emitting `CacheWriteFailure`.

## WriteStrategy

```kotlin
public enum class WriteStrategy {
    WRITE_THROUGH,
    WRITE_BACK,
}
```

| Mode | When store is called |
|------|----------------------|
| Write-through | Immediately on `put`; `remove`/`clear` mirror synchronously |
| Write-back | Buffered; flushed on interval, batch threshold, eviction, expiration, `clear()`, `remove()`, `flush()`, or dispose |

Removals are mirrored to the store only in **write-through** mode.

## EvictionStrategy

```kotlin
public interface EvictionStrategy {
    public fun <K> selectVictim(entries: Map<K, CacheEntry<*>>): K?
}
```

Built-in objects: `EvictionStrategy.Lru`, `.Lfu`, `.Fifo`.

Enum helper: `EvictionStrategyType.LRU.toStrategy()`.

## Registry

`CacheRegistry<K, V>` extends the shared `ResilienceRegistry` with cache-specific builders.

Common operations:

| Method | Description |
|--------|-------------|
| `default { }` | Sets configuration applied to subsequently created caches |
| `create(name) { }` | Creates and registers a new named cache |
| `getOrCreate(name) { }` | Returns existing cache or creates one |
| `lookup(name)` / `lookupOrNull(name)` | Retrieves a registered cache |
| `remove(name)` | Removes and disposes a cache |
| `dispose()` | Disposes registry and all registered caches |

Registry names must match `^[a-zA-Z0-9][a-zA-Z0-9._:-]*$`.

## Events

`CacheEvent` is a sealed hierarchy emitted through the component event bus.

| Event | When |
|-------|------|
| `CacheHit` | Valid entry found |
| `CacheMiss` | No valid entry (before store load / loader) |
| `CachePut` | Value stored via `put` |
| `CacheRemove` | Entry removed |
| `CacheEviction` | Entry evicted due to capacity |
| `CacheExpiration` | Entry removed due to TTL |
| `CacheLoadSuccess` / `CacheLoadFailure` | Store read-through result |
| `CacheWriteSuccess` / `CacheWriteFailure` | Store write result |
| `CacheWriteBackFlushed` | Write-back batch flushed (`count` entries) |
| `CacheCleared` | `clear()` called |
| `CacheDispose` | Instance disposed |

### Subscribing

```kotlin
import com.davils.resilience.cache.CacheEvent

// Non-reified (works across all JVM target configurations)
val job = cache.subscribe(CacheEvent::class) { event ->
    when (event) {
        is CacheEvent.CacheHit -> metrics.recordHit(event.key)
        is CacheEvent.CacheMiss -> metrics.recordMiss(event.key)
        else -> Unit
    }
}

// Cancel when done
job.cancel()
```

Filter by specific event types using separate subscriptions or `when` inside the handler.

`getOrNull` and `contains` do **not** emit hit/miss events.

## Thread safety

The cache map and write-back dirty buffer use kore `ConcurrentHashMap`. Eviction and TTL checks operate on snapshots; concurrent puts for the same key follow last-write-wins semantics.

Custom `CacheStore` implementations must be thread-safe if shared across coroutines or cache instances.
