plugins {
    `resilience-common-multiplatform`
    `resilience-core-multiplatform`
    `resilience-code-analysis`
    `resilience-compliance-security`
    `resilience-testing`
}

kreate {
    project {
        name = "davilsx-resilience-circuitbreaker"
        description = "A library for implementing the circuit breaker pattern to prevent cascading failures and improve system stability by temporarily blocking access to a failing service or resource."
    }
}

kotlin {
    android {
        namespace = "com.davils.resilience.circuitbreaker"
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
