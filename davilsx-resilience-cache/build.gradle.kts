plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-testing-multiplatform`
    `resilience-code-analysis`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-cache"
        description = "A library for caching results of operations to improve performance and reduce load on resources."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.cache"
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
