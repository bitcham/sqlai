package com.sqlai.provider

import com.sqlai.domain.datasource.DatabaseMetadata
import javax.sql.DataSource

/**
 * Strategy interface for introspecting database metadata from different sources
 *
 * Implementations of this interface extract schema information (tables, columns)
 * from various datasource types (JDBC, CSV, Parquet, etc.)
 */
interface DatabaseIntrospector {
    /**
     * Introspects the datasource and extracts metadata
     *
     * @param dataSource Spring's configured DataSource
     * @return DatabaseMetadata with all tables and columns
     * @throws MetadataSyncException if introspection fails
     */
    fun introspect(dataSource: DataSource): DatabaseMetadata
}
