# Overview

**davilsx-resilience** is a Kotlin Multiplatform library for building resilient applications. Modules provide DSL-configured components with event-driven observability and shared registry support.

## Modules

| Module | Purpose |
|--------|---------|
| [Cache](Cache.md) | Thread-safe caching with eviction, TTL, read/write-through, and write-back |
| Bulkhead | Concurrency limiting |
| Retry | Retry with backoff strategies |
| Rate limiter | Request rate control |
| Time limiter | Execution time bounds |

## Cache documentation

Start with the [Cache overview](Cache.md) for installation and quick start, then explore:

- [Configuration](Cache-Configuration.md) — convenience factories and DSL builder reference
- [API reference](Cache-API.md) — runtime operations, stores, events, registry
- [Examples](Cache-Examples.md) — practical recipes with `lruCache`, `writeThroughCache`, and more
