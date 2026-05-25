plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-testing-multiplatform`
    `resilience-code-analysis`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-timelimiter"
        description = "A library for limiting the execution time of operations to prevent long-running tasks from impacting system performance and responsiveness."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.timelimiter"
    }

    sourceSets {
        androidHostTest {
            dependencies {
                implementation(libs.bundles.resilience.tests.common.impl)
                implementation(libs.bundles.resilience.tests.jvm.impl)
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
