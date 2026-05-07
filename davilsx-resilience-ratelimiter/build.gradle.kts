plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-ratelimiter"
        description = "A library for limiting the rate of operations to prevent overwhelming resources and improve system stability."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.ratelimiter"
    }
}
