plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-circuitbreaker"))
}

kreate {
    platform {
        explicitApi = false
    }
}
