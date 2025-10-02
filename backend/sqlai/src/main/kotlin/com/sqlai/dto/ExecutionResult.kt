package com.sqlai.dto

/**
 * Result of SQL query execution
 * Contains data, metadata, and execution metrics
 */
data class ExecutionResult(
    /**
     * Execution status (SUCCESS, FAILED, TIMEOUT)
     */
    val status: ExecutionStatus,

    /**
     * Query result data as list of rows (each row is a map of column name → value)
     */
    val data: List<Map<String, Any?>>,

    /**
     * Column metadata information
     */
    val columns: List<ColumnInfo>,

    /**
     * Number of rows returned
     */
    val rowCount: Int,

    /**
     * Execution time in milliseconds
     */
    val executionTimeMs: Long,

    /**
     * Error message (null if status is SUCCESS)
     */
    val errorMessage: String? = null
)
