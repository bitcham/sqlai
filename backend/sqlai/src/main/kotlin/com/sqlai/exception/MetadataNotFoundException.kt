package com.sqlai.exception

/**
 * Exception thrown when requested metadata does not exist
 *
 * This exception is thrown when:
 * - Column metadata is not found by ID
 * - Table metadata is not found
 * - Database metadata has never been synced
 *
 * @param message Error message describing what was not found
 */
class MetadataNotFoundException(
    message: String
) : RuntimeException(message)
