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

package com.davils.resilience.common

import com.davils.kore.pattern.creational.dsl.verification.DslVerifiableData
import com.davils.resilience.common.event.ResilienceEventData

/**
 * Interface representing the base data configuration for a resilience component.
 *
 * This interface extends [DslVerifiableData] to provide validation capabilities
 * for component configurations. It ensures that every resilience component
 * has associated event handling data.
 *
 * @since 1.0.0
 */
public interface ResilienceComponentData : DslVerifiableData {
    /**
     * The configuration data for event handling within the component.
     *
     * @since 1.0.0
     */
    public val eventData: ResilienceEventData
}
