package com.sqlai.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.sqlai.domain.ai.AIProviderType
import com.sqlai.domain.ai.SqlGenerationResult
import com.sqlai.dto.*
import com.sqlai.exception.GlobalExceptionHandler
import com.sqlai.exception.QueryExecutionException
import com.sqlai.exception.UnsafeSqlException
import com.sqlai.service.ai.QueryGenerationService
import com.sqlai.service.query.QueryHistoryService
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

class QueryGenerationControllerTest : FunSpec() {

    private lateinit var queryGenerationService: QueryGenerationService
    private lateinit var queryHistoryService: QueryHistoryService
    private lateinit var controller: QueryGenerationController
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    init {
        beforeEach {
            queryGenerationService = mockk()
            queryHistoryService = mockk(relaxed = true) // relaxed = true to ignore saveQueryHistory calls
            controller = QueryGenerationController(queryGenerationService, queryHistoryService)
            mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
        }

        afterEach {
            clearAllMocks()
        }

        context("POST /api/query/generate - successful generation and execution") {

            test("should generate SQL and execute query successfully") {
                // given
                val request = GenerateQueryRequest(question = "Show all users")
                val executionResult = ExecutionResult(
                    status = ExecutionStatus.SUCCESS,
                    data = listOf(
                        mapOf("id" to 1L, "name" to "Alice"),
                        mapOf("id" to 2L, "name" to "Bob")
                    ),
                    columns = listOf(
                        ColumnInfo("id", "BIGINT"),
                        ColumnInfo("name", "VARCHAR")
                    ),
                    rowCount = 2,
                    executionTimeMs = 150
                )

                val result = SqlGenerationResult(
                    sqlStatement = "SELECT id, name FROM users",
                    explanation = "This query retrieves all users from the users table",
                    provider = AIProviderType.CLAUDE,
                    executionResult = executionResult
                )

                every { queryGenerationService.generateSql(request.question) } returns result

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.sql").value("SELECT id, name FROM users"))
                    .andExpect(jsonPath("$.explanation").value("This query retrieves all users from the users table"))
                    .andExpect(jsonPath("$.provider").value("CLAUDE"))
                    .andExpect(jsonPath("$.executionResult.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.executionResult.rowCount").value(2))
                    .andExpect(jsonPath("$.executionResult.data.length()").value(2))
                    .andExpect(jsonPath("$.executionResult.data[0].id").value(1))
                    .andExpect(jsonPath("$.executionResult.data[0].name").value("Alice"))

                verify(exactly = 1) { queryGenerationService.generateSql(request.question) }
            }

            test("should handle query with empty results") {
                // given
                val request = GenerateQueryRequest(question = "Find users with age over 200")
                val executionResult = ExecutionResult(
                    status = ExecutionStatus.SUCCESS,
                    data = emptyList(),
                    columns = listOf(
                        ColumnInfo("id", "BIGINT"),
                        ColumnInfo("name", "VARCHAR")
                    ),
                    rowCount = 0,
                    executionTimeMs = 100
                )

                val result = SqlGenerationResult(
                    sqlStatement = "SELECT id, name FROM users WHERE age > 200",
                    explanation = null,
                    provider = AIProviderType.OPENAI,
                    executionResult = executionResult
                )

                every { queryGenerationService.generateSql(request.question) } returns result

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.sql").value("SELECT id, name FROM users WHERE age > 200"))
                    .andExpect(jsonPath("$.provider").value("OPENAI"))
                    .andExpect(jsonPath("$.executionResult.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.executionResult.rowCount").value(0))
                    .andExpect(jsonPath("$.executionResult.data.length()").value(0))

                verify(exactly = 1) { queryGenerationService.generateSql(request.question) }
            }

        }

        context("POST /api/query/generate - error handling") {

            test("should return 400 for IllegalArgumentException") {
                // given
                val request = GenerateQueryRequest(question = "valid question")

                every { queryGenerationService.generateSql(any()) } throws IllegalArgumentException("Invalid input")

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Invalid input"))
            }

            test("should return 400 for unsafe SQL (INSERT)") {
                // given
                val request = GenerateQueryRequest(question = "Insert a new user")

                every { queryGenerationService.generateSql(request.question) } throws UnsafeSqlException("Only SELECT queries are allowed")

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.error").value("Unsafe SQL"))
                    .andExpect(jsonPath("$.message").value("Only SELECT queries are allowed"))
            }

            test("should return 500 for metadata unavailable") {
                // given
                val request = GenerateQueryRequest(question = "Show all users")

                every { queryGenerationService.generateSql(request.question) } throws IllegalStateException("Database metadata not available")

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isInternalServerError)
                    .andExpect(jsonPath("$.error").value("Service Error"))
                    .andExpect(jsonPath("$.message").value("Database metadata not available"))
            }

            test("should return 500 for query execution failure") {
                // given
                val request = GenerateQueryRequest(question = "Show all users")

                every { queryGenerationService.generateSql(request.question) } throws QueryExecutionException("Failed to execute query: Table does not exist")

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isInternalServerError)
                    .andExpect(jsonPath("$.error").value("Query Execution Error"))
                    .andExpect(jsonPath("$.message").value("Failed to execute query: Table does not exist"))
            }

            test("should handle TIMEOUT execution status") {
                // given
                val request = GenerateQueryRequest(question = "Show all records")
                val executionResult = ExecutionResult(
                    status = ExecutionStatus.TIMEOUT,
                    data = emptyList(),
                    columns = emptyList(),
                    rowCount = 0,
                    executionTimeMs = 30000,
                    errorMessage = "Query execution exceeded 30000ms timeout"
                )

                val result = SqlGenerationResult(
                    sqlStatement = "SELECT * FROM large_table",
                    explanation = null,
                    provider = AIProviderType.CLAUDE,
                    executionResult = executionResult
                )

                every { queryGenerationService.generateSql(request.question) } returns result

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk) // Still 200 OK, but with TIMEOUT status
                    .andExpect(jsonPath("$.executionResult.status").value("TIMEOUT"))
                    .andExpect(jsonPath("$.executionResult.errorMessage").value("Query execution exceeded 30000ms timeout"))

                verify(exactly = 1) { queryGenerationService.generateSql(request.question) }
            }

            test("should handle FAILED execution status") {
                // given
                val request = GenerateQueryRequest(question = "Show users")
                val executionResult = ExecutionResult(
                    status = ExecutionStatus.FAILED,
                    data = emptyList(),
                    columns = emptyList(),
                    rowCount = 0,
                    executionTimeMs = 50,
                    errorMessage = "Syntax error near 'FROM'"
                )

                val result = SqlGenerationResult(
                    sqlStatement = "SELECT * FORM users", // Typo: FORM instead of FROM
                    explanation = null,
                    provider = AIProviderType.OPENAI,
                    executionResult = executionResult
                )

                every { queryGenerationService.generateSql(request.question) } returns result

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk) // Still 200 OK, but with FAILED status
                    .andExpect(jsonPath("$.executionResult.status").value("FAILED"))
                    .andExpect(jsonPath("$.executionResult.errorMessage").value("Syntax error near 'FROM'"))

                verify(exactly = 1) { queryGenerationService.generateSql(request.question) }
            }
        }

        context("GET /api/query/health - health check") {

            test("should return ready when service is ready") {
                // given
                every { queryGenerationService.isReady() } returns true

                // when/then
                mockMvc.perform(get("/api/query/health"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.ready").value(true))
                    .andExpect(jsonPath("$.message").value("Service is ready"))

                verify(exactly = 1) { queryGenerationService.isReady() }
            }

            test("should return not ready when service is not ready") {
                // given
                every { queryGenerationService.isReady() } returns false

                // when/then
                mockMvc.perform(get("/api/query/health"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.ready").value(false))
                    .andExpect(jsonPath("$.message").value("Service not ready (check metadata and AI provider)"))

                verify(exactly = 1) { queryGenerationService.isReady() }
            }
        }

        context("Real-world integration scenarios") {

            test("should handle complex aggregation query with successful execution") {
                // given
                val request = GenerateQueryRequest(question = "What is the total revenue by product category?")
                val executionResult = ExecutionResult(
                    status = ExecutionStatus.SUCCESS,
                    data = listOf(
                        mapOf("category" to "Electronics", "total_revenue" to 50000.00),
                        mapOf("category" to "Clothing", "total_revenue" to 30000.00),
                        mapOf("category" to "Food", "total_revenue" to 20000.00)
                    ),
                    columns = listOf(
                        ColumnInfo("category", "VARCHAR"),
                        ColumnInfo("total_revenue", "DECIMAL")
                    ),
                    rowCount = 3,
                    executionTimeMs = 250
                )

                val result = SqlGenerationResult(
                    sqlStatement = """
                        SELECT p.category, SUM(o.total_amount) as total_revenue
                        FROM products p
                        JOIN orders o ON p.id = o.product_id
                        GROUP BY p.category
                        ORDER BY total_revenue DESC
                    """.trimIndent(),
                    explanation = "Calculates total revenue grouped by product category",
                    provider = AIProviderType.CLAUDE,
                    executionResult = executionResult
                )

                every { queryGenerationService.generateSql(request.question) } returns result

                // when/then
                mockMvc.perform(
                    post("/api/query/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.executionResult.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.executionResult.rowCount").value(3))
                    .andExpect(jsonPath("$.executionResult.data[0].category").value("Electronics"))
                    .andExpect(jsonPath("$.executionResult.data[0].total_revenue").value(50000.00))

                verify(exactly = 1) { queryGenerationService.generateSql(request.question) }
            }

        }

        context("GET /api/query/history - retrieve query history") {

            test("should retrieve recent query history") {
                // given
                val executedAt = LocalDateTime.of(2025, 10, 1, 12, 0, 0)
                val historyResponses = listOf(
                    QueryHistoryResponse(
                        naturalLanguageQueryId = 1L,
                        question = "Show all users",
                        sql = "SELECT * FROM users",
                        provider = AIProviderType.CLAUDE,
                        status = ExecutionStatus.SUCCESS,
                        rowCount = 100,
                        executedAt = executedAt
                    ),
                    QueryHistoryResponse(
                        naturalLanguageQueryId = 2L,
                        question = "Count total orders",
                        sql = "SELECT COUNT(*) FROM orders",
                        provider = AIProviderType.OPENAI,
                        status = ExecutionStatus.SUCCESS,
                        rowCount = 1,
                        executedAt = executedAt.minusMinutes(5)
                    )
                )

                every { queryHistoryService.findRecentHistory(20) } returns historyResponses

                // when/then
                mockMvc.perform(get("/api/query/history"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].question").value("Show all users"))
                    .andExpect(jsonPath("$[0].sql").value("SELECT * FROM users"))
                    .andExpect(jsonPath("$[0].provider").value("CLAUDE"))
                    .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                    .andExpect(jsonPath("$[0].rowCount").value(100))
                    .andExpect(jsonPath("$[1].question").value("Count total orders"))
                    .andExpect(jsonPath("$[1].provider").value("OPENAI"))

                verify(exactly = 1) { queryHistoryService.findRecentHistory(20) }
            }

            test("should retrieve history with custom limit") {
                // given
                val historyResponses = listOf(
                    QueryHistoryResponse(
                        naturalLanguageQueryId = 1L,
                        question = "Query 1",
                        sql = "SELECT 1",
                        provider = AIProviderType.CLAUDE,
                        status = ExecutionStatus.SUCCESS,
                        rowCount = 1,
                        executedAt = LocalDateTime.now()
                    )
                )

                every { queryHistoryService.findRecentHistory(5) } returns historyResponses

                // when/then
                mockMvc.perform(get("/api/query/history?limit=5"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(1))

                verify(exactly = 1) { queryHistoryService.findRecentHistory(5) }
            }

            test("should return empty list when no history exists") {
                // given
                every { queryHistoryService.findRecentHistory(20) } returns emptyList()

                // when/then
                mockMvc.perform(get("/api/query/history"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(0))

                verify(exactly = 1) { queryHistoryService.findRecentHistory(20) }
            }
        }
    }
}
