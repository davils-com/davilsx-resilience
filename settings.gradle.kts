plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "davilsx-resilience"

include(":davilsx-resilience-retry")
include(":davilsx-resilience-cache")
include(":davilsx-resilience-bulkhead")
include(":davilsx-resilience-timelimiter")
include(":davilsx-resilience-ratelimiter")
include(":davilsx-resilience-circuitbreaker")
include(":davilsx-resilience-common")
include(":davilsx-resilience-micrometer")
include(":davilsx-resilience-ktor")
include(":davilsx-resilience-metrics")

// Example
include(":davilsx-resilience-example")
include(":davilsx-resilience-example:example-retry")
include(":davilsx-resilience-example:example-bulkhead")
include(":davilsx-resilience-example:example-circuitbreaker")
include(":davilsx-resilience-example:example-ratelimiter")
