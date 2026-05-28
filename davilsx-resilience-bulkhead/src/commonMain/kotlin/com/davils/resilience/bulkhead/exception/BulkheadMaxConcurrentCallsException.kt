package com.davils.resilience.bulkhead.exception

public class BulkheadMaxConcurrentCallsException(public val msg: String) : Exception(msg)
