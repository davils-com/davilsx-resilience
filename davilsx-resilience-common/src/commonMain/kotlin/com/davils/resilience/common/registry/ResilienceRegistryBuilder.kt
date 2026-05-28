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

package com.davils.resilience.common.registry

import com.davils.kore.pattern.creational.dsl.validation.DslValidator
import com.davils.resilience.common.event.ResilienceEventBuilder

/**
 * A builder class for creating [ResilienceRegistryData] instances.
 *
 * This builder provides a DSL for configuring the registry, including its event bus.
 *
 * @since 1.0.0
 */
public class ResilienceRegistryBuilder : DslValidator<ResilienceRegistryData>() {
    private val eventBuilder = ResilienceEventBuilder()

    /**
     * Configures the event bus for the registry.
     *
     * @param builder A lambda block to configure the event bus.
     * @since 1.0.0
     */
    public fun event(builder: ResilienceEventBuilder.() -> Unit) {
        eventBuilder.apply(builder)
    }

    /**
     * Produces the [ResilienceRegistryData] instance.
     *
     * @return The configured registry data.
     * @since 1.0.0
     */
    override fun data(): ResilienceRegistryData {
        return ResilienceRegistryData(
            eventData = eventBuilder.produce()
        )
    }
}
