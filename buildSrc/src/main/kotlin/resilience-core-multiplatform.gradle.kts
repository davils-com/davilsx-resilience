/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import com.davils.buildSrc.Project

plugins {
    com.android.kotlin.multiplatform.library
    org.jetbrains.kotlin.multiplatform
    com.davils.kreate
}

group = Project.Identity.GROUP

kreate {
    project {
        platform {
            javaVersion = JavaVersion.VERSION_17
            explicitApi = true
            allWarningsAsErrors = true
        }
    }
}

kotlin {
    jvm()

    wasmJs {
        browser()
    }

    js(IR) {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    mingwX64()
    macosArm64()

    linuxX64()
    linuxArm64()

    tvosArm64()
    tvosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    android {
        compileSdk { version = release(Project.Android.COMPILE_SDK) }
        minSdk = Project.Android.MIN_SDK
        withJava()

        withHostTest {
            isIncludeAndroidResources = true
        }
    }
}
