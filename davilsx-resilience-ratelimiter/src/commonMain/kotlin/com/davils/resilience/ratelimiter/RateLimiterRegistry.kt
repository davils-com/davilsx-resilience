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

package com.davils.resilience.ratelimiter

import com.davils.resilience.common.registry.ResilienceRegistry
import com.davils.resilience.common.registry.ResilienceRegistryBuilder
import com.davils.resilience.common.registry.ResilienceRegistryData

public class RateLimiterRegistry(
    override val registryData: ResilienceRegistryData
) : ResilienceRegistry<RateLimiterEvent, RateLimiterData, RateLimiterBuilder, RateLimiter>() {
    override fun createBuilder(): RateLimiterBuilder = RateLimiterBuilder()

    override fun createComponent(data: RateLimiterData): RateLimiter = RateLimiter(data)
}

public fun rateLimiterRegistry(builder: ResilienceRegistryBuilder.() -> Unit): RateLimiterRegistry {
    val registryBuilder = ResilienceRegistryBuilder()
    registryBuilder.builder()
    val data = registryBuilder.produce()
    return RateLimiterRegistry(data)
}
