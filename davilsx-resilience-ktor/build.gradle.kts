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
    }
}

dependencies {
    implementation(libs.bundles.resilience.ktor.common.impl)
    implementation(project(":davilsx-resilience-ratelimiter"))
    implementation(project(":davilsx-resilience-timelimiter"))

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.resilience.tests.common.impl)
    testImplementation(libs.bundles.resilience.tests.jvm.impl)
}
