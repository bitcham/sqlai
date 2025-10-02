package com.sqlai.dto

import com.sqlai.domain.ai.AIProviderType
import java.time.LocalDateTime

/**
 * Response DTO for query history.
 * Combines data from NaturalLanguageQuery, GeneratedSqlQuery, and QueryExecution.
 */
data class QueryHistoryResponse(
    val naturalLanguageQueryId: Long,
    val question: String,
    val sql: String,
    val provider: AIProviderType,
    val status: ExecutionStatus,
    val rowCount: Int,
    val executedAt: LocalDateTime
)
