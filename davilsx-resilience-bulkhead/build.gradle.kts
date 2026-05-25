plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-testing-multiplatform`
    `resilience-code-analysis`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-bulkhead"
        description = "A library for limiting the number of concurrent operations to prevent resource exhaustion and improve system stability."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.bulkhead"
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
