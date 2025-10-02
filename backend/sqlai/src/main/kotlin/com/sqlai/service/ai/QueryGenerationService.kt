package com.sqlai.service.ai

import com.sqlai.provider.AIProvider
import com.sqlai.domain.ai.SqlGenerationResult
import com.sqlai.dto.ExecutionStatus
import com.sqlai.service.datasource.GetDatabaseMetadataService
import com.sqlai.service.query.QueryExecutionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for orchestrating SQL query generation and execution from natural language
 *
 * Flow:
 * 1. Get DatabaseMetadata (from DataSource domain)
 * 2. Format schema (SchemaFormatter)
 * 3. Build prompt (PromptBuilder)
 * 4. Call AI provider (AIProvider)
 * 5. Execute SQL (QueryExecutionService)
 * 6. Return combined result (SqlGenerationResult with execution data)
 */
@Service
@Transactional(readOnly = true)
class QueryGenerationService(
    private val getDatabaseMetadataService: GetDatabaseMetadataService,
    private val schemaFormatter: SchemaFormatter,
    private val promptBuilder: PromptBuilder,
    private val aiProvider: AIProvider,
    private val queryExecutionService: QueryExecutionService
) {

    private val logger = LoggerFactory.getLogger(QueryGenerationService::class.java)

    /**
     * Generate SQL query from natural language input and execute it automatically
     *
     * @param userInput Natural language question from user
     * @return SQL generation result with query, explanation, provider info, and execution result
     * @throws IllegalStateException if metadata not available or AI call fails
     */
    fun generateSql(userInput: String): SqlGenerationResult {
        require(userInput.isNotBlank()) { "User input must not be blank" }

        logger.info("Generating SQL for user input: $userInput")

        // Step 1: Get database metadata
        val metadata = getDatabaseMetadataService.execute()
            ?: throw IllegalStateException("No database metadata available. Run metadata sync first.")

        logger.debug("Retrieved metadata for schema: ${metadata.schemaName}")

        // Step 2 & 3: Format schema and build prompt
        val prompt = promptBuilder.buildPrompt(userInput, metadata)
        logger.debug("Built prompt (length: ${prompt.length} chars)")

        // Step 4: Call AI provider
        val result = aiProvider.generateSql(prompt)
        logger.info("Successfully generated SQL using ${result.provider}")

        // Step 5: Execute SQL
        val executionResult = try {
            logger.info("Executing generated SQL automatically")
            queryExecutionService.execute(result.sqlStatement)
        } catch (e: Exception) {
            logger.error("Failed to execute generated SQL: ${e.message}", e)
            // Return result with failed execution status
            return result.copy(
                executionResult = com.sqlai.dto.ExecutionResult(
                    status = ExecutionStatus.FAILED,
                    data = emptyList(),
                    columns = emptyList(),
                    rowCount = 0,
                    executionTimeMs = 0,
                    errorMessage = e.message
                )
            )
        }

        logger.info("Query generation and execution completed successfully")

        return result.copy(executionResult = executionResult)
    }

    /**
     * Check if the service is ready to generate queries
     * Requires: metadata synced and AI provider available
     */
    fun isReady(): Boolean {
        val hasMetadata = getDatabaseMetadataService.execute() != null
        val providerAvailable = aiProvider.isAvailable()

        return hasMetadata && providerAvailable
    }
}
