# Overview

**davilsx-resilience** is a Kotlin Multiplatform library for building resilient applications. Modules provide DSL-configured components with event-driven observability and shared registry support.

## Modules

| Module | Purpose |
|--------|---------|
| [Cache](Cache.md) | Thread-safe caching with eviction, TTL, read/write-through, and write-back |
| [Circuit Breaker](Circuit-Breaker.md) | Failure protection with sliding-window thresholds and state machine |
| Bulkhead | Concurrency limiting |
| Retry | Retry with backoff strategies |
| [Rate limiter](Rate-Limiter.md) | Request rate control |
| [Time limiter](Time-Limiter.md) | Execution time bounds |

## Cache documentation

Start with the [Cache overview](Cache.md) for installation and quick start, then explore:

- [Configuration](Cache-Configuration.md) — convenience factories and DSL builder reference
- [API reference](Cache-API.md) — runtime operations, stores, events, registry
- [Examples](Cache-Examples.md) — practical recipes with `lruCache`, `writeThroughCache`, and more

## Circuit Breaker documentation

Start with the [Circuit Breaker overview](Circuit-Breaker.md) for installation and quick start, then explore:

- [Configuration](Circuit-Breaker-Configuration.md) — DSL settings, defaults, and validation
- [API reference](Circuit-Breaker-API.md) — runtime operations, events, metrics, registry
- [Examples](Circuit-Breaker-Examples.md) — practical recipes for protection, predicates, and monitoring
- [Internals](Circuit-Breaker-Internals.md) — architecture and component reference for contributors

## Rate Limiter documentation

Start with the [Rate Limiter overview](Rate-Limiter.md) for installation and quick start, then explore:

- [Configuration](Rate-Limiter-Configuration.md) — DSL settings, strategies, defaults, and validation
- [API reference](Rate-Limiter-API.md) — runtime operations, events, metrics, registry, Ktor plugin
- [Examples](Rate-Limiter-Examples.md) — practical recipes with `fixedRateLimiter`, `slidingWindowRateLimiter`, and more

## Time Limiter documentation

Start with the [Time Limiter overview](Time-Limiter.md) for installation and quick start, then explore:

- [Configuration](Time-Limiter-Configuration.md) — DSL settings, strategies, defaults, and validation
- [API reference](Time-Limiter-API.md) — runtime operations, events, metrics, registry, Ktor plugin
- [Examples](Time-Limiter-Examples.md) — practical recipes with `hardTimeLimiter`, `softTimeLimiter`, and more
