package com.sqlai.service.query

import com.sqlai.domain.ai.AIProviderType
import com.sqlai.domain.ai.SqlGenerationResult
import com.sqlai.domain.query.GeneratedSqlQuery
import com.sqlai.domain.query.NaturalLanguageQuery
import com.sqlai.domain.query.QueryExecution
import com.sqlai.dto.ExecutionResult
import com.sqlai.dto.ExecutionStatus
import com.sqlai.dto.QueryHistoryResponse
import com.sqlai.repository.GeneratedSqlQueryRepository
import com.sqlai.repository.NaturalLanguageQueryRepository
import com.sqlai.repository.QueryExecutionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing query history.
 * Saves query execution history and provides retrieval functionality.
 */
@Service
class QueryHistoryService(
    private val naturalLanguageQueryRepository: NaturalLanguageQueryRepository,
    private val generatedSqlQueryRepository: GeneratedSqlQueryRepository,
    private val queryExecutionRepository: QueryExecutionRepository
) {
    /**
     * Saves query history (natural language question, generated SQL, and execution result).
     * This method is called after each query execution.
     */
    @Transactional
    fun saveQueryHistory(
        question: String,
        sqlResult: SqlGenerationResult,
        executionResult: ExecutionResult
    ): QueryHistoryResponse {
        // 1. Save natural language query
        val nlQuery = NaturalLanguageQuery(question = question)
        naturalLanguageQueryRepository.save(nlQuery)

        // 2. Save generated SQL
        val sqlQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = nlQuery.id!!,
            sqlStatement = sqlResult.sqlStatement,
            explanation = sqlResult.explanation,
            aiProvider = sqlResult.provider
        )
        generatedSqlQueryRepository.save(sqlQuery)

        // 3. Save execution result
        val execution = QueryExecution(
            generatedSqlQueryId = sqlQuery.id!!,
            status = executionResult.status,
            resultRowCount = executionResult.rowCount,
            executionTimeMs = executionResult.executionTimeMs,
            errorMessage = executionResult.errorMessage
        )
        queryExecutionRepository.save(execution)

        return QueryHistoryResponse(
            naturalLanguageQueryId = nlQuery.id!!,
            question = nlQuery.question,
            sql = sqlQuery.sqlStatement,
            provider = sqlQuery.aiProvider,
            status = execution.status,
            rowCount = execution.resultRowCount,
            executedAt = execution.executedAt
        )
    }

    /**
     * Retrieves recent query history.
     * @param limit Maximum number of queries to return (default: 20)
     */
    @Transactional(readOnly = true)
    fun findRecentHistory(limit: Int = 20): List<QueryHistoryResponse> {
        val recentQueries = naturalLanguageQueryRepository.findAll(
            PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        )

        return recentQueries.content.map { nlQuery ->
            val sqlQuery = generatedSqlQueryRepository.findByNaturalLanguageQueryId(nlQuery.id!!).firstOrNull()
            val execution = sqlQuery?.let {
                queryExecutionRepository.findByGeneratedSqlQueryId(it.id!!).firstOrNull()
            }

            QueryHistoryResponse(
                naturalLanguageQueryId = nlQuery.id!!,
                question = nlQuery.question,
                sql = sqlQuery?.sqlStatement ?: "",
                provider = sqlQuery?.aiProvider ?: AIProviderType.CLAUDE,
                status = execution?.status ?: ExecutionStatus.FAILED,
                rowCount = execution?.resultRowCount ?: 0,
                executedAt = execution?.executedAt ?: nlQuery.createdAt
            )
        }
    }
}
