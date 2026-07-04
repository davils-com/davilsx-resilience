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

package com.davils.resilience.circuitbreaker.exception

/**
 * Thrown internally when a call result matches the configured `recordResultPredicate`.
 *
 * The circuit breaker wraps the result in this exception so that it is counted as a failure
 * in the sliding window, while still propagating enough information for event subscribers.
 *
 * @param result The result value that was classified as a failure.
 * @since 1.0.0
 */
public class ResultRecordedAsFailureException(
    public val result: Any?,
    message: String = "Result was recorded as a failure by the circuit breaker"
) : Exception(message)
