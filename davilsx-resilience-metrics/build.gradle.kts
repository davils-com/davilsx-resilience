plugins {
    `resilience-core`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
    `resilience-common-multiplatform`
}

kreate {
    project {
        name = "davilsx-resilience-metrics"
        description = "A library for collecting and exposing metrics related to the resilience patterns implemented in the davilsx-resilience suite, allowing developers to monitor and analyze the performance and behavior of their applications under various conditions."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.metrics"
    }

    sourceSets {
        androidHostTest {
            dependencies {
                implementation(libs.bundles.resilience.tests.common.impl)
                implementation(libs.bundles.resilience.tests.jvm.impl)
            }
        }

        commonMain {
            dependencies {
                implementation(libs.bundles.resilience.shared.impl)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.bundles.resilience.tests.common.impl)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.bundles.resilience.tests.jvm.impl)
            }
        }
    }
}
