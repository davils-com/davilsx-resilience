plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-ktor"
        description = "A library for integrating resilience patterns with Ktor HTTP client to enhance the robustness of network operations."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.ktor"
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
