package com.sqlai.dto

/**
 * Status of SQL query execution
 */
enum class ExecutionStatus {
    /**
     * Query executed successfully
     */
    SUCCESS,

    /**
     * Query execution failed due to SQL error or exception
     */
    FAILED,

    /**
     * Query execution exceeded timeout limit
     */
    TIMEOUT
}
