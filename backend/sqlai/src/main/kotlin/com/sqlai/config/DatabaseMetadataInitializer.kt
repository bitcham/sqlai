package com.sqlai.config

import com.sqlai.service.datasource.SyncDatabaseMetadataService
import com.sqlai.exception.MetadataSyncException
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * ApplicationRunner for automatic database metadata sync on startup
 *
 * This component executes once when the Spring Boot application starts,
 * after all beans are initialized but before accepting HTTP requests.
 *
 * It ensures the application has fresh metadata from the configured
 * datasource before users can generate SQL queries.
 */
@Component
class DatabaseMetadataInitializer(
    private val syncService: SyncDatabaseMetadataService
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        logger.info("╔════════════════════════════════════════════════════════╗")
        logger.info("║   Database Metadata Initialization Started            ║")
        logger.info("╚════════════════════════════════════════════════════════╝")

        try {
            val metadata = syncService.execute()

            logger.info("╔════════════════════════════════════════════════════════╗")
            logger.info("║   ✅ Application Ready!                                ║")
            logger.info("║   Database: ${metadata.schemaName.padEnd(40)}║")
            logger.info("║   Tables: ${metadata.tables.size.toString().padEnd(42)}║")
            logger.info("║   Columns: ${metadata.tables.sumOf { it.columns.size }.toString().padEnd(41)}║")
            logger.info("╚════════════════════════════════════════════════════════╝")

        } catch (e: MetadataSyncException) {
            logger.error("╔════════════════════════════════════════════════════════╗")
            logger.error("║   ❌ FATAL: Metadata Sync Failed                       ║")
            logger.error("║   Application cannot start without database metadata   ║")
            logger.error("╚════════════════════════════════════════════════════════╝")
            logger.error("Error details:", e)

            // Fail-fast: Prevent application from starting
            throw IllegalStateException("Cannot start application without database metadata", e)

        } catch (e: Exception) {
            logger.error("╔════════════════════════════════════════════════════════╗")
            logger.error("║   ❌ FATAL: Unexpected error during initialization     ║")
            logger.error("╚════════════════════════════════════════════════════════╝")
            logger.error("Error details:", e)

            throw IllegalStateException("Unexpected error during application startup", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DatabaseMetadataInitializer::class.java)
    }
}
