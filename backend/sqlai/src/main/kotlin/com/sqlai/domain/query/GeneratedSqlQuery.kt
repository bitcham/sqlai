package com.sqlai.domain.query

import com.sqlai.domain.ai.AIProviderType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Represents an AI-generated SQL query.
 * Links to the original natural language question.
 */
@Entity
@Table(name = "generated_sql_queries")
class GeneratedSqlQuery(
    @Column(name = "natural_language_query_id", nullable = false)
    val naturalLanguageQueryId: Long,

    @Column(name = "sql_statement", nullable = false, columnDefinition = "TEXT")
    val sqlStatement: String,

    @Column(name = "explanation", columnDefinition = "TEXT")
    val explanation: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_provider", nullable = false, length = 20)
    val aiProvider: AIProviderType,

    @Column(name = "generated_at", nullable = false)
    val generatedAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    init {
        require(sqlStatement.isNotBlank()) { "SQL statement cannot be blank" }
    }
}
