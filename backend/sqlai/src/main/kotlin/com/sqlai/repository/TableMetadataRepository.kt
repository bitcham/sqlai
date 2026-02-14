package com.sqlai.repository

import com.sqlai.domain.datasource.TableMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for TableMetadata entity
 * Manages table metadata within a database schema
 */
@Repository
interface TableMetadataRepository : JpaRepository<TableMetadata, Long> {

    /**
     * Find all tables for a given database metadata
     * @param metadataId the database metadata ID
     * @return list of tables
     */
    @Query("SELECT t FROM TableMetadata t WHERE t.id IN (SELECT t2.id FROM DatabaseMetadata m JOIN m.tables t2 WHERE m.id = :metadataId)")
    fun findByMetadataId(@Param("metadataId") metadataId: Long): List<TableMetadata>

    /**
     * Find a specific table by metadata ID and table name
     * @param metadataId the database metadata ID
     * @param tableName the table name
     * @return TableMetadata if found, null otherwise
     */
    @Query("SELECT t FROM TableMetadata t WHERE t.id IN (SELECT t2.id FROM DatabaseMetadata m JOIN m.tables t2 WHERE m.id = :metadataId) AND t.tableName = :tableName")
    fun findByMetadataIdAndTableName(@Param("metadataId") metadataId: Long, @Param("tableName") tableName: String): TableMetadata?

    /**
     * Find all tables with their columns eagerly loaded (Query 2 of 2-step fetch)
     * Used together with DatabaseMetadataRepository.findFirstWithTables() to avoid
     * both N+1 and Cartesian product problems
     * @param tables the tables to load columns for
     * @return list of tables with columns loaded
     */
    @Query("SELECT DISTINCT t FROM TableMetadata t LEFT JOIN FETCH t.columns WHERE t IN :tables")
    fun findAllWithColumns(@Param("tables") tables: List<TableMetadata>): List<TableMetadata>
}
