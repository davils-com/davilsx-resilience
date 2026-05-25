plugins {
    `resilience-common-jvm`
    `resilience-core-jvm`
    `resilience-testing-jvm`
}

dependencies {
    testImplementation(libs.bundles.resilience.tests.common.impl)
    testImplementation(libs.bundles.resilience.tests.jvm.impl)
}
