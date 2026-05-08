package com.davils.resilience.retry

import kotlinx.coroutines.sync.Mutex

public class RetryAsync(override val data: RetryData) : RetryProvider {
    private val mutex = Mutex()

    public suspend fun <T> execute(block: suspend () -> T): T {
        return block()
    }
}

public fun retryAsync(builder: RetryBuilder.() -> Unit): RetryAsync {
    val retryBuilder = RetryBuilder()
    retryBuilder.builder()
    val data = retryBuilder.build()
    return RetryAsync(data)
}
