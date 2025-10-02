package com.sqlai.controller

import com.sqlai.dto.GenerateQueryRequest
import com.sqlai.dto.GenerateQueryResponse
import com.sqlai.dto.QueryHistoryResponse
import com.sqlai.service.ai.QueryGenerationService
import com.sqlai.service.query.QueryHistoryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API controller for SQL query generation and history
 *
 * Endpoints:
 * - POST /api/query/generate - Generate and execute SQL query
 * - GET /api/query/history - Retrieve recent query history
 * - GET /api/query/health - Health check
 */
@RestController
@RequestMapping("/api/query")
class QueryGenerationController(
    private val queryGenerationService: QueryGenerationService,
    private val queryHistoryService: QueryHistoryService
) {

    private val logger = LoggerFactory.getLogger(QueryGenerationController::class.java)

    /**
     * Generate SQL query from natural language question and execute it automatically.
     * Query history is automatically saved for future reference.
     *
     * @param request User's natural language question
     * @return Generated SQL with explanation, provider info, and execution result
     */
    @PostMapping("/generate")
    fun generateQuery(
        @Valid @RequestBody request: GenerateQueryRequest
    ): ResponseEntity<GenerateQueryResponse> {
        logger.info("Received query generation request: ${request.question}")

        val result = queryGenerationService.generateSql(request.question)

        // Save to history
        if (result.executionResult != null) {
            queryHistoryService.saveQueryHistory(
                question = request.question,
                sqlResult = result,
                executionResult = result.executionResult!!
            )
            logger.debug("Query history saved for: ${request.question}")
        }

        val response = GenerateQueryResponse(
            sql = result.sqlStatement,
            explanation = result.explanation,
            provider = result.provider,
            executionResult = result.executionResult
        )

        logger.info("Successfully generated and executed SQL using ${result.provider}")
        logger.debug("Execution status: {}, rows: {}", result.executionResult?.status, result.executionResult?.rowCount)

        return ResponseEntity.ok(response)
    }

    /**
     * Health check endpoint to verify service is ready
     *
     * @return Service readiness status
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val isReady = queryGenerationService.isReady()

        return ResponseEntity.ok(
            mapOf(
                "ready" to isReady,
                "message" to if (isReady) "Service is ready" else "Service not ready (check metadata and AI provider)"
            )
        )
    }

    /**
     * Retrieve recent query history
     *
     * @param limit Maximum number of queries to return (default: 20)
     * @return List of recent query history
     */
    @GetMapping("/history")
    fun getHistory(
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<List<QueryHistoryResponse>> {
        logger.debug("Retrieving query history with limit: $limit")

        val history = queryHistoryService.findRecentHistory(limit)

        logger.info("Retrieved ${history.size} query history records")

        return ResponseEntity.ok(history)
    }
}
