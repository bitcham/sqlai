package com.sqlai.provider

import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import com.sqlai.exception.MetadataSyncException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.File
import javax.sql.DataSource

/**
 * CSV-based database introspector
 *
 * Reads CSV files from a configured directory and infers schema information
 * by parsing headers and analyzing data types from sample rows.
 *
 * Each CSV file represents a table, with the filename as the table name.
 */
@Component
@ConditionalOnProperty(name = arrayOf("datasource.type"), havingValue = "csv")
class CsvDatabaseIntrospector(
    @Value("\${csv.data.directory:/data/csv}") private val csvDirectory: String
) : DatabaseIntrospector {

    override fun introspect(dataSource: DataSource): DatabaseMetadata {
        logger.info("Starting CSV file introspection from directory: $csvDirectory")

        try {
            val directory = File(csvDirectory)

            if (!directory.exists()) {
                throw MetadataSyncException("CSV directory does not exist: $csvDirectory")
            }

            if (!directory.isDirectory) {
                throw MetadataSyncException("CSV path is not a directory: $csvDirectory")
            }

            val csvFiles = directory.listFiles { file ->
                file.isFile && file.extension.lowercase() == "csv"
            } ?: emptyArray()

            if (csvFiles.isEmpty()) {
                logger.warn("No CSV files found in directory: $csvDirectory")
            }

            logger.info("Found ${csvFiles.size} CSV files")

            val tables = csvFiles.map { file -> parseTable(file) }

            logger.info("Discovered ${tables.size} tables with ${tables.sumOf { it.columns.size }} total columns")

            return DatabaseMetadata(
                schemaName = "csv_datasource"
            ).apply {
                this.tables.addAll(tables)
            }

        } catch (e: MetadataSyncException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to introspect CSV files", e)
            throw MetadataSyncException("CSV introspection failed: ${e.message}", e)
        }
    }

    private fun parseTable(file: File): TableMetadata {
        logger.debug("Parsing CSV file: ${file.name}")

        try {
            val lines = file.readLines()

            if (lines.isEmpty()) {
                logger.warn("CSV file is empty: ${file.name}")
                return TableMetadata(tableName = file.nameWithoutExtension)
            }

            // Parse header line
            val headerLine = lines.first()
            val columnNames = headerLine.split(",").map { it.trim() }

            if (columnNames.isEmpty()) {
                logger.warn("CSV file has no columns: ${file.name}")
                return TableMetadata(tableName = file.nameWithoutExtension)
            }

            // Infer data types from sample rows (first 10 rows)
            val sampleRows = lines.drop(1).take(10).map { line ->
                line.split(",").map { it.trim() }
            }

            val columns = columnNames.mapIndexed { index, name ->
                val dataType = inferDataType(sampleRows, index)

                ColumnMetadata(
                    columnName = name,
                    dataType = dataType,
                    isNullable = true,  // CSV doesn't have strict nullability
                    isPrimaryKey = false,
                    isForeignKey = false
                )
            }

            return TableMetadata(
                tableName = file.nameWithoutExtension
            ).apply {
                this.columns.addAll(columns)
            }

        } catch (e: Exception) {
            logger.error("Failed to parse CSV file: ${file.name}", e)
            throw MetadataSyncException("Failed to parse CSV file ${file.name}: ${e.message}", e)
        }
    }

    /**
     * Infers data type from sample values in a column
     *
     * Heuristic rules:
     * - If all values are integers → INTEGER
     * - If all values are numbers (with decimals) → DECIMAL
     * - If values look like dates (yyyy-MM-dd) → DATE
     * - Otherwise → TEXT
     */
    private fun inferDataType(sampleRows: List<List<String>>, columnIndex: Int): String {
        if (sampleRows.isEmpty()) {
            return "TEXT"
        }

        val values = sampleRows.mapNotNull { row ->
            row.getOrNull(columnIndex)?.takeIf { it.isNotBlank() }
        }

        if (values.isEmpty()) {
            return "TEXT"
        }

        // Check if all values are integers
        if (values.all { it.toIntOrNull() != null }) {
            return "INTEGER"
        }

        // Check if all values are numbers (including decimals)
        if (values.all { it.toDoubleOrNull() != null }) {
            return "DECIMAL"
        }

        // Check if values look like dates (simple pattern: yyyy-MM-dd)
        val datePattern = Regex("""^\d{4}-\d{2}-\d{2}$""")
        if (values.all { datePattern.matches(it) }) {
            return "DATE"
        }

        // Default to TEXT
        return "TEXT"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CsvDatabaseIntrospector::class.java)
    }
}
