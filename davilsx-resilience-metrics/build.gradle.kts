plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-testing-multiplatform`
    `resilience-code-analysis-multiplatform`
    `resilience-compliance-security`
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
                implementation(project(":davilsx-resilience-retry"))
                implementation(project(":davilsx-resilience-bulkhead"))
                implementation(project(":davilsx-resilience-circuitbreaker"))
                implementation(project(":davilsx-resilience-cache"))
                implementation(project(":davilsx-resilience-ratelimiter"))
                implementation(project(":davilsx-resilience-timelimiter"))
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
