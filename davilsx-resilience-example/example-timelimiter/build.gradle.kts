plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-timelimiter"))
}

kreate {
    platform {
        explicitApi = false
    }
}
