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

package com.davils.resilience.metrics.ratelimiter

import com.davils.resilience.metrics.MetricsCollector
import com.davils.resilience.ratelimiter.RateLimiter
import com.davils.resilience.ratelimiter.RateLimiterMetrics
import kotlinx.coroutines.runBlocking

/**
 * Collects metrics from a [RateLimiter] component.
 *
 * @since 1.0.0
 */
public class RateLimiterMetricsCollector internal constructor(
    override val component: RateLimiter,
) : MetricsCollector<RateLimiter>() {
    private var latestMetrics: RateLimiterMetrics? = null

    override fun scrape() {
        latestMetrics = runBlocking { component.getMetrics() }
    }

    /**
     * Scrapes and returns the latest metrics snapshot.
     *
     * @since 1.0.0
     */
    public fun refresh(): RateLimiterMetrics {
        scrape()
        return latestMetrics ?: runBlocking { component.getMetrics() }
    }

    /**
     * Returns the most recently scraped metrics snapshot, or `null` if [refresh] has not been called.
     *
     * @since 1.0.0
     */
    public fun metrics(): RateLimiterMetrics? = latestMetrics
}
