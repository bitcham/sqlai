package com.sqlai.service.datasource

import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.repository.DatabaseMetadataRepository
import com.sqlai.repository.TableMetadataRepository
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
    private val metadataRepository: DatabaseMetadataRepository,
    private val tableMetadataRepository: TableMetadataRepository
) {

    /**
     * Retrieves the current database metadata using a 2-step fetch join strategy
     * to avoid both N+1 queries and Cartesian product problems.
     *
     * Query 1: DatabaseMetadata + tables (fetch join)
     * Query 2: tables + columns (fetch join with IN clause)
     *
     * Hibernate persistence context merges the results automatically
     * within the same transaction.
     *
     * @return DatabaseMetadata entity if exists, null otherwise
     */
    @Transactional(readOnly = true)
    fun execute(): DatabaseMetadata? {
        logger.debug("Retrieving database metadata...")

        val metadata = metadataRepository.findFirstWithTables()

        if (metadata == null) {
            logger.warn("No database metadata found - metadata sync may not have been performed")
        } else {
            if (metadata.tables.isNotEmpty()) {
                tableMetadataRepository.findAllWithColumns(metadata.tables)
            }
            logger.debug("Metadata found: ${metadata.schemaName} with ${metadata.tables.size} tables")
        }

        return metadata
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GetDatabaseMetadataService::class.java)
    }
}
