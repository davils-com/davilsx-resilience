plugins {
    `resilience-core-multiplatform`
    `resilience-code-analysis`
    `resilience-testing-multiplatform`
    `resilience-compliance-security`
}

kreate {
    project {
        name = "davilsx-resilience-common"
        description = "A library for common utilities and abstractions used across resilience modules."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.common"
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
                api(libs.bundles.resilience.common.common.api)
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
