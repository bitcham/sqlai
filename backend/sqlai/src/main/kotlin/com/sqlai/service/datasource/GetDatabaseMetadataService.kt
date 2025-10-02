package com.sqlai.service.datasource

import com.sqlai.repository.DatabaseMetadataRepository
import com.sqlai.domain.datasource.DatabaseMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for retrieving database metadata
 *
 * This service queries the persisted database metadata to provide
 * schema information to the AI for SQL query generation.
 */
@Service
class GetDatabaseMetadataService(
    private val metadataRepository: DatabaseMetadataRepository
) {

    /**
     * Retrieves the current database metadata
     *
     * @return DatabaseMetadata entity if exists, null otherwise
     */
    @Transactional(readOnly = true)
    fun execute(): DatabaseMetadata? {
        logger.debug("Retrieving database metadata...")

        val metadata = metadataRepository.findAll().firstOrNull()

        if (metadata == null) {
            logger.warn("No database metadata found - metadata sync may not have been performed")
        } else {
            logger.debug("Metadata found: ${metadata.schemaName} with ${metadata.tables.size} tables")
        }

        return metadata
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GetDatabaseMetadataService::class.java)
    }
}
