package com.sqlai.service.query

import com.sqlai.domain.ai.AIProviderType
import com.sqlai.domain.ai.SqlGenerationResult
import com.sqlai.dto.ColumnInfo
import com.sqlai.dto.ExecutionResult
import com.sqlai.dto.ExecutionStatus
import com.sqlai.repository.GeneratedSqlQueryRepository
import com.sqlai.repository.NaturalLanguageQueryRepository
import com.sqlai.repository.QueryExecutionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryHistoryService::class)
class QueryHistoryServiceTest {

    @Autowired
    private lateinit var queryHistoryService: QueryHistoryService

    @Autowired
    private lateinit var naturalLanguageQueryRepository: NaturalLanguageQueryRepository

    @Autowired
    private lateinit var generatedSqlQueryRepository: GeneratedSqlQueryRepository

    @Autowired
    private lateinit var queryExecutionRepository: QueryExecutionRepository

    @AfterEach
    fun cleanup() {
        queryExecutionRepository.deleteAll()
        generatedSqlQueryRepository.deleteAll()
        naturalLanguageQueryRepository.deleteAll()
    }

    @Test
    fun `should save complete query history`() {
        // given
        val question = "Show all users"
        val sqlResult = SqlGenerationResult(
            sqlStatement = "SELECT * FROM users",
            explanation = "Retrieves all users",
            provider = AIProviderType.CLAUDE,
            executionResult = null
        )
        val executionResult = ExecutionResult(
            status = ExecutionStatus.SUCCESS,
            data = emptyList(),
            columns = listOf(ColumnInfo("id", "BIGINT"), ColumnInfo("name", "VARCHAR")),
            rowCount = 100,
            executionTimeMs = 250L,
            errorMessage = null
        )

        // when
        val response = queryHistoryService.saveQueryHistory(question, sqlResult, executionResult)

        // then
        assertThat(response.question).isEqualTo(question)
        assertThat(response.sql).isEqualTo(sqlResult.sqlStatement)
        assertThat(response.provider).isEqualTo(AIProviderType.CLAUDE)
        assertThat(response.status).isEqualTo(ExecutionStatus.SUCCESS)
        assertThat(response.rowCount).isEqualTo(100)
        assertThat(response.executedAt).isNotNull()

        // Verify entities are persisted
        assertThat(naturalLanguageQueryRepository.count()).isEqualTo(1)
        assertThat(generatedSqlQueryRepository.count()).isEqualTo(1)
        assertThat(queryExecutionRepository.count()).isEqualTo(1)
    }

    @Test
    fun `should save query history with failed execution`() {
        // given
        val question = "Show invalid table"
        val sqlResult = SqlGenerationResult(
            sqlStatement = "SELECT * FROM invalid_table",
            explanation = null,
            provider = AIProviderType.OPENAI,
            executionResult = null
        )
        val executionResult = ExecutionResult(
            status = ExecutionStatus.FAILED,
            data = emptyList(),
            columns = emptyList(),
            rowCount = 0,
            executionTimeMs = 50L,
            errorMessage = "Table not found: invalid_table"
        )

        // when
        val response = queryHistoryService.saveQueryHistory(question, sqlResult, executionResult)

        // then
        assertThat(response.status).isEqualTo(ExecutionStatus.FAILED)
        assertThat(response.rowCount).isEqualTo(0)
    }

    @Test
    fun `should retrieve recent history`() {
        // given
        saveMultipleHistoryRecords(5)

        // when
        val history = queryHistoryService.findRecentHistory(limit = 3)

        // then
        assertThat(history).hasSize(3)
        // History should be ordered by createdAt DESC (most recent first)
        assertThat(history[0].question).contains("Question 5")
        assertThat(history[1].question).contains("Question 4")
        assertThat(history[2].question).contains("Question 3")
    }

    @Test
    fun `should retrieve all history when limit is greater than record count`() {
        // given
        saveMultipleHistoryRecords(3)

        // when
        val history = queryHistoryService.findRecentHistory(limit = 10)

        // then
        assertThat(history).hasSize(3)
    }

    @Test
    fun `should return empty list when no history exists`() {
        // when
        val history = queryHistoryService.findRecentHistory()

        // then
        assertThat(history).isEmpty()
    }

    @Test
    fun `should use default limit of 20 when not specified`() {
        // given
        saveMultipleHistoryRecords(25)

        // when
        val history = queryHistoryService.findRecentHistory()

        // then
        assertThat(history).hasSize(20)
    }

    @Test
    fun `should handle query history with different AI providers`() {
        // given
        val providers = listOf(AIProviderType.CLAUDE, AIProviderType.OPENAI, AIProviderType.GEMINI)

        providers.forEach { provider ->
            val sqlResult = SqlGenerationResult(
                sqlStatement = "SELECT 1",
                explanation = null,
                provider = provider,
                executionResult = null
            )
            val executionResult = ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                data = emptyList(),
                columns = emptyList(),
                rowCount = 1,
                executionTimeMs = 50L,
                errorMessage = null
            )
            queryHistoryService.saveQueryHistory("Question for $provider", sqlResult, executionResult)
        }

        // when
        val history = queryHistoryService.findRecentHistory()

        // then
        assertThat(history).hasSize(3)
        assertThat(history.map { it.provider }).containsExactlyInAnyOrder(
            AIProviderType.CLAUDE,
            AIProviderType.OPENAI,
            AIProviderType.GEMINI
        )
    }

    private fun saveMultipleHistoryRecords(count: Int) {
        repeat(count) { index ->
            val sqlResult = SqlGenerationResult(
                sqlStatement = "SELECT * FROM table_$index",
                explanation = "Query $index",
                provider = AIProviderType.CLAUDE,
                executionResult = null
            )
            val executionResult = ExecutionResult(
                status = ExecutionStatus.SUCCESS,
                data = emptyList(),
                columns = emptyList(),
                rowCount = index * 10,
                executionTimeMs = 100L,
                errorMessage = null
            )
            queryHistoryService.saveQueryHistory("Question ${index + 1}", sqlResult, executionResult)

            // Add small delay to ensure different timestamps
            Thread.sleep(10)
        }
    }
}
