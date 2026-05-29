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

package com.davils.resilience.metrics

import com.davils.resilience.common.ResilienceComponent
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex

public abstract class MetricsCollector<C : ResilienceComponent<*, *>> {
    protected abstract val component: C
    private var isActive = atomic(false)
    protected val mutex: Mutex = Mutex()
    protected abstract fun scrape()
    public fun collect() {
        if (!isActive.value) return
        scrape()
    }
}
