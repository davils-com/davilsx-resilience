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

package com.davils.resilience.circuitbreaker.internal

/** Classification of a single call result used by the sliding window. */
internal enum class CallOutcome {
    SUCCESS,
    ERROR,
    SLOW_SUCCESS,
    SLOW_ERROR;

    val isSuccess: Boolean get() = this == SUCCESS || this == SLOW_SUCCESS
    val isError: Boolean get() = this == ERROR || this == SLOW_ERROR
    val isSlow: Boolean get() = this == SLOW_SUCCESS || this == SLOW_ERROR
}
