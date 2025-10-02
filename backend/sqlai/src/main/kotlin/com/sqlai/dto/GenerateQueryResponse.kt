package com.sqlai.dto

import com.sqlai.domain.ai.AIProviderType

/**
 * Response DTO for SQL query generation and execution
 */
data class GenerateQueryResponse(
    /**
     * Generated SQL query
     */
    val sql: String,

    /**
     * Explanation of the query
     */
    val explanation: String?,

    /**
     * AI provider used for generation
     */
    val provider: AIProviderType,

    /**
     * Query execution result (null if execution failed or not performed)
     */
    val executionResult: ExecutionResult? = null
)
