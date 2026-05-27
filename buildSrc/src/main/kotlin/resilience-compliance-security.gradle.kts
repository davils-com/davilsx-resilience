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

import com.davils.kreate.module.trivy.LicenseSeverity
import com.davils.kreate.module.trivy.Score
import com.davils.kreate.module.trivy.SecretSeverity

plugins {
    com.davils.kreate
}

dependencyLocking {
    lockAllConfigurations()
}

kreate {
    trivy {
        enabled = true

        vulnerability {
            score = listOf(Score.CRITICAL, Score.HIGH, Score.MEDIUM, Score.LOW)
            failOnFindings = true
            lockFiles.from(
                fileTree(projectDir) {
                    include("*.lockfile")
                }
            )
        }

        license {
            severity = listOf(LicenseSeverity.CRITICAL, LicenseSeverity.HIGH, LicenseSeverity.UNKNOWN)
            failOnForbidden = true
            ignoredLicenses = listOf("MIT")
            lockFiles.from(
                fileTree(projectDir) {
                    include("*.lockfile")
                }
            )
        }

        secrets {
            severity = listOf(SecretSeverity.CRITICAL, SecretSeverity.HIGH, SecretSeverity.MEDIUM, SecretSeverity.LOW)
            failOnFindings = true
            secretConfig = rootProject.layout.projectDirectory.file("trivy-secret.yaml")
            sourceFiles.from(
                fileTree(projectDir) {
                    include(
                        "src/**/*.kt",
                        "src/**/*.java",
                        "**/*.yaml",
                        "**/*.yml",
                        "**/*.env",
                        "**/*.properties",
                        "**/*.json"
                    )
                }
            )
        }
    }
}
