package com.sqlai.domain.query

import com.sqlai.dto.ExecutionStatus
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Represents the execution result of a generated SQL query.
 * Links to the generated SQL query and stores execution metrics.
 */
@Entity
@Table(name = "query_executions")
class QueryExecution(
    @Column(name = "generated_sql_query_id", nullable = false)
    val generatedSqlQueryId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ExecutionStatus,

    @Column(name = "result_row_count", nullable = false)
    val resultRowCount: Int,

    @Column(name = "execution_time_ms", nullable = false)
    val executionTimeMs: Long,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String?,

    @Column(name = "executed_at", nullable = false)
    val executedAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    init {
        require(resultRowCount >= 0) { "Result row count cannot be negative" }
        require(executionTimeMs >= 0) { "Execution time cannot be negative" }
    }
}
