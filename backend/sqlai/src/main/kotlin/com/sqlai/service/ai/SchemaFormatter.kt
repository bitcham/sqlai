package com.sqlai.service.ai

import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import org.springframework.stereotype.Service

/**
 * Service for formatting database schema metadata into structured plain text
 * Used by PromptBuilder to include schema context in AI prompts
 */
@Service
class SchemaFormatter {

    /**
     * Format DatabaseMetadata into structured plain text for AI prompt
     * Includes all tables with columns, primary keys, and foreign key relationships
     *
     * Example output:
     * ```
     * Table: customers
     *   Columns:
     *     - id: BIGINT (Primary Key)
     *     - name: VARCHAR(255)
     *     - email: VARCHAR(255)
     *
     * Table: orders
     *   Columns:
     *     - id: BIGINT (Primary Key)
     *     - customer_id: BIGINT (Foreign Key -> customers.id)
     *     - order_date: DATE
     * ```
     */
    fun format(metadata: DatabaseMetadata): String {
        val builder = StringBuilder()

        metadata.tables.forEachIndexed { index, table ->
            if (index > 0) {
                builder.append("\n")
            }
            builder.append(formatTable(table))
        }

        return builder.toString()
    }

    private fun formatTable(table: TableMetadata): String {
        val builder = StringBuilder()
        builder.append("Table: ${table.tableName}\n")
        builder.append("  Columns:\n")

        table.columns.forEach { column ->
            builder.append("    - ${formatColumn(column)}\n")
        }

        return builder.toString()
    }

    private fun formatColumn(column: ColumnMetadata): String {
        val parts = mutableListOf<String>()

        // Column name and type
        parts.add("${column.columnName}: ${column.dataType}")

        // Add key information
        when {
            column.isPrimaryKey -> parts.add("(Primary Key)")
            column.hasForeignKeyRelation() -> {
                val fkRef = column.getForeignKeyReference()
                parts.add("(Foreign Key -> $fkRef)")
            }
        }

        return parts.joinToString(" ")
    }
}
