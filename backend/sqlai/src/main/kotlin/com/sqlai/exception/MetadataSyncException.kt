package com.sqlai.exception

/**
 * Exception thrown when database metadata synchronization fails
 *
 * This exception wraps underlying errors from JDBC introspection,
 * file I/O (for CSV), or repository operations.
 *
 * @param message Error message describing the failure
 * @param cause Original exception that caused the sync failure
 */
class MetadataSyncException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
