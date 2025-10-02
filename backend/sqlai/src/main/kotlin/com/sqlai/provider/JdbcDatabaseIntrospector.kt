package com.sqlai.provider

import com.sqlai.exception.MetadataSyncException
import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * JDBC-based database introspector
 *
 * Uses JDBC DatabaseMetaData API to introspect schema information
 * from relational databases (MySQL, PostgreSQL, etc.)
 *
 * Only syncs TABLE objects (ignores VIEWs, SYSTEM TABLEs, etc.)
 */
@Component
@ConditionalOnProperty(name = arrayOf("datasource.type"), havingValue = "jdbc", matchIfMissing = true)
class JdbcDatabaseIntrospector : DatabaseIntrospector {

    override fun introspect(dataSource: DataSource): DatabaseMetadata {
        logger.info("Starting JDBC database introspection...")

        try {
            dataSource.connection.use { connection ->
                val dbMetaData = connection.metaData
                val databaseName = getDatabaseName(dbMetaData)
                val schemaName = getSchemaName(connection)

                logger.info("Introspecting database: $databaseName, schema: $schemaName")

                val tables = extractTables(dbMetaData, schemaName)

                logger.info("Discovered ${tables.size} tables with ${tables.sumOf { it.columns.size }} total columns")

                return DatabaseMetadata(
                    schemaName = schemaName
                ).apply {
                    this.tables.addAll(tables)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to introspect JDBC database", e)
            throw MetadataSyncException("JDBC introspection failed: ${e.message}", e)
        }
    }

    private fun getDatabaseName(metaData: DatabaseMetaData): String {
        return try {
            "${metaData.databaseProductName} ${metaData.databaseProductVersion}"
        } catch (e: Exception) {
            "Unknown Database"
        }
    }

    private fun getSchemaName(connection: java.sql.Connection): String {
        return try {
            // H2: schema is "PUBLIC", catalog is database name
            // PostgreSQL/MySQL: schema is the actual schema name
            val schema = connection.schema ?: connection.catalog ?: "PUBLIC"
            logger.info("Detected schema: $schema (catalog: ${connection.catalog})")
            schema.uppercase()
        } catch (e: Exception) {
            "PUBLIC"
        }
    }

    private fun extractTables(metaData: DatabaseMetaData, schemaName: String): List<TableMetadata> {
        val tables = mutableListOf<TableMetadata>()

        // Only sync TABLE objects (ignore VIEWs)
        metaData.getTables(null, schemaName, "%", arrayOf("TABLE")).use { rs ->
            while (rs.next()) {
                val tableName = rs.getString("TABLE_NAME")
                logger.debug("Processing table: $tableName")

                val columns = extractColumns(metaData, schemaName, tableName)

                if (columns.isNotEmpty()) {
                    tables.add(TableMetadata(
                        tableName = tableName
                    ).apply {
                        this.columns.addAll(columns)
                    })
                }
            }
        }

        return tables
    }

    private fun extractColumns(
        metaData: DatabaseMetaData,
        schemaName: String,
        tableName: String
    ): List<ColumnMetadata> {
        val columns = mutableListOf<ColumnMetadata>()
        val primaryKeys = extractPrimaryKeys(metaData, schemaName, tableName)
        val foreignKeys = extractForeignKeys(metaData, schemaName, tableName)

        metaData.getColumns(null, schemaName, tableName, "%").use { rs ->
            while (rs.next()) {
                val columnName = rs.getString("COLUMN_NAME")
                val dataType = rs.getString("TYPE_NAME")
                val isNullable = rs.getString("IS_NULLABLE") == "YES"

                columns.add(ColumnMetadata(
                    columnName = columnName,
                    dataType = dataType,
                    isNullable = isNullable,
                    isPrimaryKey = primaryKeys.contains(columnName),
                    isForeignKey = foreignKeys.containsKey(columnName),
                    referencedTable = foreignKeys[columnName]?.first,
                    referencedColumn = foreignKeys[columnName]?.second
                ))
            }
        }

        return columns
    }

    private fun extractPrimaryKeys(
        metaData: DatabaseMetaData,
        schemaName: String,
        tableName: String
    ): Set<String> {
        val primaryKeys = mutableSetOf<String>()

        try {
            metaData.getPrimaryKeys(null, schemaName, tableName).use { rs ->
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"))
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract primary keys for table $tableName: ${e.message}")
        }

        return primaryKeys
    }

    private fun extractForeignKeys(
        metaData: DatabaseMetaData,
        schemaName: String,
        tableName: String
    ): Map<String, Pair<String, String>> {
        val foreignKeys = mutableMapOf<String, Pair<String, String>>()

        try {
            metaData.getImportedKeys(null, schemaName, tableName).use { rs ->
                while (rs.next()) {
                    val columnName = rs.getString("FKCOLUMN_NAME")
                    val referencedTable = rs.getString("PKTABLE_NAME")
                    val referencedColumn = rs.getString("PKCOLUMN_NAME")

                    foreignKeys[columnName] = Pair(referencedTable, referencedColumn)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract foreign keys for table $tableName: ${e.message}")
        }

        return foreignKeys
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbcDatabaseIntrospector::class.java)
    }
}
