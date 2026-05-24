rootProject.name = "davilsx-resilience"

include(":davilsx-resilience-retry")
include(":davilsx-resilience-cache")
include(":davilsx-resilience-bulkhead")
include(":davilsx-resilience-timelimiter")
include(":davilsx-resilience-ratelimiter")
include(":davilsx-resilience-circuitbreaker")
include(":davilsx-resilience-common")
include(":davilsx-resilience-micrometer")

include("davilsx-resilience-ktor")
include("davilsx-resilience-metrics")