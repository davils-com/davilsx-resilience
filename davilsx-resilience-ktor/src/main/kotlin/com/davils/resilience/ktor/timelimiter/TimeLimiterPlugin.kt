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

package com.davils.resilience.ktor.timelimiter

import com.davils.resilience.timelimiter.TimeLimiter
import com.davils.resilience.timelimiter.TimeoutExceededException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.Hook
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelinePhase

/**
 * Configuration for the [TimeLimiterPlugin].
 *
 * @since 1.0.0
 */
public class TimeLimiterPluginConfig {
    public var timeLimiter: TimeLimiter? = null
}

private class TimeLimiterCallHook(
    private val limiter: TimeLimiter,
) : Hook<suspend (ApplicationCall) -> Unit> {
    private val phase: PipelinePhase = PipelinePhase("TimeLimiter")

    override fun install(
        pipeline: ApplicationCallPipeline,
        handler: suspend (ApplicationCall) -> Unit,
    ) {
        val timeLimiter = limiter
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, phase)
        pipeline.intercept(phase) {
            try {
                timeLimiter.execute {
                    proceed()
                }
            } catch (_: TimeoutExceededException) {
                if (!context.response.isSent) {
                    context.respond(HttpStatusCode.GatewayTimeout, "Request timed out")
                }
                finish()
            }
        }
    }
}

/**
 * Ktor plugin that enforces execution time limits for request handling.
 *
 * Wraps the call pipeline in [TimeLimiter.execute]. When the timeout is exceeded,
 * responds with HTTP 504 Gateway Timeout.
 *
 * @since 1.0.0
 */
public val TimeLimiterPlugin: ApplicationPlugin<TimeLimiterPluginConfig> = createApplicationPlugin(
    name = "TimeLimiter",
    createConfiguration = ::TimeLimiterPluginConfig,
) {
    val limiter = pluginConfig.timeLimiter
        ?: error("TimeLimiterPlugin requires a TimeLimiter instance via timeLimiter { ... }")

    on(TimeLimiterCallHook(limiter)) { }
}
