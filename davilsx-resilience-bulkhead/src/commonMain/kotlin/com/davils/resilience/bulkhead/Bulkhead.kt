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

package com.davils.resilience.bulkhead

import com.davils.resilience.bulkhead.event.BulkheadEvent
import com.davils.resilience.common.ResilienceComponent

public class Bulkhead internal constructor(
    override val data: BulkheadData
) : ResilienceComponent<BulkheadData, BulkheadEvent>() {
    override val disposeEvent: BulkheadEvent
        get() = BulkheadEvent.BulkheadDispose
}

public fun bulkhead(builder: BulkheadBuilder.() -> Unit): Bulkhead {
    val bulkheadBuilder = BulkheadBuilder()
    bulkheadBuilder.apply(builder)
    val data = bulkheadBuilder.produce()
    return Bulkhead(data)
}
