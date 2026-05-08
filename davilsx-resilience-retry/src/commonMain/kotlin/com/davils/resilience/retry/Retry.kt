package com.davils.resilience.retry

import kotlinx.atomicfu.locks.SynchronizedObject

public class Retry(override val data: RetryData) : RetryProvider, SynchronizedObject() {
    public fun <T> execute(block: () -> T): T {
        return block()
    }
}

public fun retry(builder: RetryBuilder.() -> Unit): Retry {
    val retryBuilder = RetryBuilder()
    retryBuilder.builder()
    val data = retryBuilder.build()
    return Retry(data)
}
