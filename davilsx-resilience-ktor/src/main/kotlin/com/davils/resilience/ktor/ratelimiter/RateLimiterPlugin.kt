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

package com.davils.resilience.ktor.ratelimiter

import com.davils.resilience.ratelimiter.RateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.header
import io.ktor.server.response.respond
import kotlin.math.ceil

/**
 * Configuration for the [RateLimiterPlugin].
 *
 * @since 1.0.0
 */
public class RateLimiterPluginConfig {
    public var rateLimiter: RateLimiter? = null
    public var permitsPerRequest: Int = 1
}

/**
 * Ktor plugin that enforces rate limits before request handling.
 *
 * Rejects requests with HTTP 429 and a `Retry-After` header when the configured
 * [RateLimiter] cannot grant a permit.
 *
 * @since 1.0.0
 */
public val RateLimiterPlugin: ApplicationPlugin<RateLimiterPluginConfig> = createApplicationPlugin(
    name = "RateLimiter",
    createConfiguration = ::RateLimiterPluginConfig,
) {
    val limiter = pluginConfig.rateLimiter
        ?: error("RateLimiterPlugin requires a RateLimiter instance via rateLimiter { ... }")

    onCall { call ->
        if (limiter.tryAcquire(pluginConfig.permitsPerRequest)) {
            return@onCall
        }

        val waitDuration = limiter.reserveSlot(pluginConfig.permitsPerRequest)
        val retryAfterSeconds = if (waitDuration.isPositive()) {
            ceil(waitDuration.inWholeMilliseconds / 1000.0).toLong().coerceAtLeast(1L)
        } else {
            1L
        }
        call.response.header("Retry-After", retryAfterSeconds.toString())
        call.respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded")
    }
}
