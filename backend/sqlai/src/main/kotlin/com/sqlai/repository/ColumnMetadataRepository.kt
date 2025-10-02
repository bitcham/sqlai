package com.sqlai.repository

import com.sqlai.domain.datasource.ColumnMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for ColumnMetadata entity
 * Manages column metadata within tables
 */
@Repository
interface ColumnMetadataRepository : JpaRepository<ColumnMetadata, Long> {

    /**
     * Find all columns for a given table
     * @param tableMetadataId the table metadata ID
     * @return list of columns
     */
    @Query("SELECT c FROM ColumnMetadata c WHERE c.id IN (SELECT c2.id FROM TableMetadata t JOIN t.columns c2 WHERE t.id = :tableMetadataId)")
    fun findByTableMetadataId(@Param("tableMetadataId") tableMetadataId: Long): List<ColumnMetadata>
}
