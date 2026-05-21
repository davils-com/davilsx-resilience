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

package com.davils.resilience.retry

import com.davils.resilience.common.registry.Registry

/**
 * A specialized [Registry] for storing and managing [Retry] instances.
 *
 * This registry allows for centralized management of retry configurations across the application.
 * Being an `object`, it provides a single global point of access for retry mechanisms.
 *
 * @since 1.0.0
 */
public object RetryRegistry : Registry<Retry>()
