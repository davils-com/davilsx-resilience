plugins {
    `resilience-core-jvm`
}

dependencies {
    implementation(project(":davilsx-resilience-retry"))
}

kreate {
    platform {
        explicitApi = false
    }
}
