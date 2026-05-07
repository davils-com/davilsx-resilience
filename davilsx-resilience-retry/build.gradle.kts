plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-retry"
        description = "A library for retrying failed operations."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.retry"
    }
}
