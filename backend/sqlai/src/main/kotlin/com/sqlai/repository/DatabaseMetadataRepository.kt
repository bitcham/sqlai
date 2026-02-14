package com.sqlai.repository

import com.sqlai.domain.datasource.DatabaseMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository for DatabaseMetadata entity
 * Manages metadata for the configured data source
 */
@Repository
interface DatabaseMetadataRepository : JpaRepository<DatabaseMetadata, Long> {

    /**
     * Find metadata by schema name
     * @param schemaName the schema name to search for
     * @return DatabaseMetadata if found, null otherwise
     */
    fun findBySchemaName(schemaName: String): DatabaseMetadata?

    /**
     * Find metadata with all tables eagerly loaded (N+1 prevention)
     * @return DatabaseMetadata with tables, null if not found
     */
    @Query("SELECT DISTINCT m FROM DatabaseMetadata m LEFT JOIN FETCH m.tables")
    fun findFirstWithTables(): DatabaseMetadata?

}
