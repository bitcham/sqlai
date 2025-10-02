package com.sqlai.exception

/**
 * Exception thrown when SQL query contains unsafe operations
 * Only SELECT queries are allowed for safety
 */
class UnsafeSqlException(
    message: String
) : RuntimeException(message)
