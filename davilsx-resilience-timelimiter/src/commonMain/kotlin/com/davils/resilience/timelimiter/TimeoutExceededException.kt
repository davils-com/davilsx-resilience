package com.davils.resilience.timelimiter

public class TimeoutExceededException(public val timeoutMs: Long, message: String? = null) : RuntimeException(message ?: "Execution exceeded timeout of ${timeoutMs}ms")

