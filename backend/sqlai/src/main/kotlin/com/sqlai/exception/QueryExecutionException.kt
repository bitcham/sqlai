package com.sqlai.exception

/**
 * Exception thrown when SQL query execution fails
 */
class QueryExecutionException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
