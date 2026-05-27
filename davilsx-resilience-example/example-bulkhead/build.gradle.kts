plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-bulkhead"))
}

kreate {
    platform {
        explicitApi = false
    }
}
