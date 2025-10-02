package com.sqlai.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for SQL query execution policies
 *
 * Safety features:
 * - Timeout protection (prevent long-running queries)
 * - Row limit enforcement (prevent excessive data retrieval)
 * - Operation whitelist (only allow SELECT for safety)
 */
@ConfigurationProperties(prefix = "execution")
data class ExecutionPolicyProperties(
    /**
     * Maximum execution time in milliseconds (default: 30 seconds)
     */
    val maxExecutionTimeMs: Long = 30000,

    /**
     * Maximum number of rows to return (default: 1000 rows)
     */
    val maxRowLimit: Int = 1000,

    /**
     * List of allowed SQL operations (default: SELECT only)
     */
    val allowedOperations: List<String> = listOf("SELECT"),

    /**
     * JDBC statement timeout in seconds (default: 30 seconds)
     */
    val queryTimeoutSeconds: Int = 30
)
