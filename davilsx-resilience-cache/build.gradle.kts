plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-cache"
        description = "A library for caching results of operations to improve performance and reduce load on resources."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.cache"
    }
}
