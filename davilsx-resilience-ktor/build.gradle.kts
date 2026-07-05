plugins {
    `resilience-common-jvm`
    `resilience-core-jvm`
    `resilience-testing-jvm`
    `resilience-code-analysis-jvm`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-ktor"
        description = "Ktor integration for davilsx-resilience components."
        platform {
            javaVersion = JavaVersion.toVersion(23)
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core:${libs.versions.ktor.get()}")
    implementation(project(":davilsx-resilience-ratelimiter"))
    implementation(project(":davilsx-resilience-timelimiter"))

    testImplementation("io.ktor:ktor-server-test-host:${libs.versions.ktor.get()}")
    testImplementation(libs.bundles.resilience.tests.common.impl)
    testImplementation(libs.bundles.resilience.tests.jvm.impl)
}
