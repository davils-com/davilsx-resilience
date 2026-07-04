plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-ratelimiter"))
}

kreate {
    platform {
        explicitApi = false
    }
}
