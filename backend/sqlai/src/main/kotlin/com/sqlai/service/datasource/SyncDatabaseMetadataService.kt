package com.sqlai.service.datasource

import com.sqlai.provider.DatabaseIntrospector
import com.sqlai.repository.DatabaseMetadataRepository
import com.sqlai.exception.MetadataSyncException
import com.sqlai.domain.datasource.DatabaseMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

/**
 * Service for syncing database metadata from configured datasource
 *
 * This service orchestrates the metadata synchronization process:
 * 1. Introspects the datasource (via DatabaseIntrospector strategy)
 * 2. Persists metadata to application database
 * 3. Returns synced metadata for verification
 */
@Service
class SyncDatabaseMetadataService(
    private val introspector: DatabaseIntrospector,
    private val metadataRepository: DatabaseMetadataRepository,
    private val dataSource: DataSource
) {

    /**
     * Syncs metadata from configured datasource
     *
     * @return DatabaseMetadata entity with all tables and columns
     * @throws MetadataSyncException if synchronization fails
     */
    @Transactional
    fun execute(): DatabaseMetadata {
        logger.info("=== Starting database metadata sync ===")

        try {
            // Step 1: Introspect datasource
            val metadata = introspector.introspect(dataSource)

            // Step 2: Clear existing metadata (fresh sync)
            metadataRepository.deleteAll()

            // Step 3: Persist new metadata
            val saved = metadataRepository.save(metadata)

            logger.info("✅ Metadata sync completed successfully:")
            logger.info("   - Schema: ${saved.schemaName}")
            logger.info("   - Tables: ${saved.tables.size}")
            logger.info("   - Total Columns: ${saved.tables.sumOf { it.columns.size }}")

            return saved

        } catch (e: MetadataSyncException) {
            logger.error("❌ Metadata sync failed", e)
            throw e
        } catch (e: Exception) {
            logger.error("❌ Unexpected error during metadata sync", e)
            throw MetadataSyncException("Failed to sync database metadata: ${e.message}", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SyncDatabaseMetadataService::class.java)
    }
}
