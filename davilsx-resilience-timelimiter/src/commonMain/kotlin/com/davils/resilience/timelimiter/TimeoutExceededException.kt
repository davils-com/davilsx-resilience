package com.davils.resilience.timelimiter

public class TimeoutExceededException(timeoutMs: Long, message: String? = null) :
    RuntimeException(message ?: "Execution exceeded timeout of ${timeoutMs}ms")

