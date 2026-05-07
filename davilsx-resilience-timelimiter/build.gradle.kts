plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-timelimiter"
        description = "A library for limiting the execution time of operations to prevent long-running tasks from impacting system performance and responsiveness."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.timelimiter"
    }
}
