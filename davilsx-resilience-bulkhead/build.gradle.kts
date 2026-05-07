plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-bulkhead"
        description = "A library for limiting the number of concurrent operations to prevent resource exhaustion and improve system stability."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.bulkhead"
    }
}
