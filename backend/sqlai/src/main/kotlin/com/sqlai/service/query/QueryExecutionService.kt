package com.sqlai.service.query

import com.sqlai.config.ExecutionPolicyProperties
import com.sqlai.dto.ColumnInfo
import com.sqlai.dto.ExecutionResult
import com.sqlai.dto.ExecutionStatus
import com.sqlai.exception.QueryExecutionException
import com.sqlai.exception.UnsafeSqlException
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.sql.SQLTimeoutException

/**
 * Service for executing SQL queries against configured datasource
 *
 * Features:
 * - SQL safety validation (SELECT-only)
 * - Query execution with timeout
 * - ResultSet conversion to JSON-friendly format
 * - Metrics tracking (execution time, row count)
 */
@Service
class QueryExecutionService(
    private val jdbcTemplate: JdbcTemplate,
    private val executionPolicyProperties: ExecutionPolicyProperties
) {

    private val logger = LoggerFactory.getLogger(QueryExecutionService::class.java)

    /**
     * Execute SQL query and return results
     *
     * @param sqlStatement SQL query to execute
     * @return ExecutionResult with status, data, columns, and metrics
     * @throws UnsafeSqlException if SQL contains unsafe operations
     * @throws QueryExecutionException if execution fails
     */
    fun execute(sqlStatement: String): ExecutionResult {
        require(sqlStatement.isNotBlank()) { "SQL statement must not be blank" }

        logger.info("Executing SQL query")
        logger.debug("SQL: $sqlStatement")

        // Validate SQL safety
        validateSqlSafety(sqlStatement)

        // Remove comments before execution
        val cleanedSql = removeComments(sqlStatement)
        logger.debug("Cleaned SQL: $cleanedSql")

        val startTime = System.currentTimeMillis()

        return try {
            // Execute query with timeout
            var resultData: List<Map<String, Any?>> = emptyList()
            var resultColumns: List<ColumnInfo> = emptyList()

            jdbcTemplate.query(cleanedSql) { rs ->
                val (data, columns) = convertResultSet(rs, executionPolicyProperties.maxRowLimit)
                resultData = data
                resultColumns = columns
            }

            val executionTime = System.currentTimeMillis() - startTime

            logger.info("Query executed successfully: ${resultData.size} rows in ${executionTime}ms")

            ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                data = resultData,
                columns = resultColumns,
                rowCount = resultData.size,
                executionTimeMs = executionTime,
                errorMessage = null
            )

        } catch (e: SQLTimeoutException) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.warn("Query execution timeout after ${executionTime}ms")

            ExecutionResult(
                status = ExecutionStatus.TIMEOUT,
                data = emptyList(),
                columns = emptyList(),
                rowCount = 0,
                executionTimeMs = executionTime,
                errorMessage = "Query execution exceeded ${executionPolicyProperties.maxExecutionTimeMs}ms timeout"
            )

        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error("Query execution failed: ${e.message}", e)

            throw QueryExecutionException("Failed to execute query: ${e.message}", e)
        }
    }

    /**
     * Remove SQL comments (both line comments and block comments)
     *
     * @param sqlStatement SQL query with potential comments
     * @return SQL query without comments
     */
    private fun removeComments(sqlStatement: String): String {
        return sqlStatement
            .lines()
            .filterNot { it.trim().startsWith("--") }  // Remove line comments
            .joinToString("\n")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")  // Remove block comments
            .trim()
    }

    /**
     * Validate SQL safety (SELECT-only check)
     *
     * @param sqlStatement SQL query to validate
     * @return true if SQL is safe
     * @throws UnsafeSqlException if SQL contains unsafe operations
     */
    fun validateSqlSafety(sqlStatement: String): Boolean {
        // Remove comments and normalize
        val normalized = removeComments(sqlStatement).trim().uppercase()

        // Check if starts with SELECT or WITH (for CTEs)
        if (!normalized.startsWith("SELECT") && !normalized.startsWith("WITH")) {
            throw UnsafeSqlException("Only SELECT queries are allowed")
        }

        // Check for dangerous keywords
        val dangerousKeywords = listOf(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "TRUNCATE", "EXEC", "EXECUTE", "CALL", "GRANT", "REVOKE"
        )

        for (keyword in dangerousKeywords) {
            if (normalized.contains(Regex("\\b$keyword\\b"))) {
                throw UnsafeSqlException("Query contains unsafe operation: $keyword")
            }
        }

        logger.debug("SQL validation passed")
        return true
    }

    /**
     * Convert JDBC ResultSet to list of maps with column metadata
     *
     * @param resultSet JDBC ResultSet to convert
     * @param maxRows Maximum number of rows to read
     * @return Pair of (data rows, column metadata)
     */
    private fun convertResultSet(
        resultSet: ResultSet,
        maxRows: Int
    ): Pair<List<Map<String, Any?>>, List<ColumnInfo>> {
        val metaData = resultSet.metaData
        val columnCount = metaData.columnCount

        // Extract column info
        val columns = (1..columnCount).map { i ->
            ColumnInfo(
                name = metaData.getColumnName(i),
                type = metaData.getColumnTypeName(i)
            )
        }

        // Convert rows
        val data = mutableListOf<Map<String, Any?>>()
        var rowCount = 0

        while (resultSet.next() && rowCount < maxRows) {
            val row = columns.associate { column ->
                column.name to resultSet.getObject(column.name)
            }
            data.add(row)
            rowCount++
        }

        logger.debug("Converted ${data.size} rows (max: $maxRows)")

        return Pair(data, columns)
    }
}
