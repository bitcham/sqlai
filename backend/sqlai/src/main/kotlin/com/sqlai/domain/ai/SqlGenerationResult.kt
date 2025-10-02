package com.sqlai.domain.ai

import com.sqlai.dto.ExecutionResult

/**
 * Result of SQL query generation and execution
 * This is a DTO (not persisted to database)
 */
data class SqlGenerationResult(
    /**
     * Generated SQL query statement
     */
    val sqlStatement: String,

    /**
     * Explanation of the query from AI
     */
    val explanation: String? = null,

    /**
     * AI provider used for generation
     */
    val provider: AIProviderType,

    /**
     * Query execution result (null if not executed yet)
     */
    val executionResult: ExecutionResult? = null
)
