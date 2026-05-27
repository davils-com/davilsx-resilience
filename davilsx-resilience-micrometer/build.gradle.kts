plugins {
    `resilience-common-jvm`
    `resilience-core-jvm`
    `resilience-testing-jvm`
    `resilience-code-analysis-jvm`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-micrometer"
        description = "A library for integrating resilience patterns with Micrometer metrics to provide insights into the performance and reliability of operations."
    }
}

dependencies {
    implementation(libs.bundles.resilience.micrometer.jvm.impl)

    testImplementation(libs.bundles.resilience.tests.common.impl)
    testImplementation(libs.bundles.resilience.tests.jvm.impl)
}
