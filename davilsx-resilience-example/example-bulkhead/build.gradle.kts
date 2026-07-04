plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-bulkhead"))
    implementation(project(":davilsx-resilience-cache"))
}

kreate {
    platform {
        explicitApi = false
    }
}
