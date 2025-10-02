package com.sqlai.domain.query

import com.sqlai.dto.ExecutionStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class QueryExecutionTest {

    @Test
    fun `should create QueryExecution with valid parameters`() {
        // given
        val generatedSqlQueryId = 1L
        val status = ExecutionStatus.SUCCESS
        val rowCount = 100
        val executionTimeMs = 250L
        val errorMessage: String? = null

        // when
        val execution = QueryExecution(
            generatedSqlQueryId = generatedSqlQueryId,
            status = status,
            resultRowCount = rowCount,
            executionTimeMs = executionTimeMs,
            errorMessage = errorMessage
        )

        // then
        assertThat(execution.generatedSqlQueryId).isEqualTo(generatedSqlQueryId)
        assertThat(execution.status).isEqualTo(status)
        assertThat(execution.resultRowCount).isEqualTo(rowCount)
        assertThat(execution.executionTimeMs).isEqualTo(executionTimeMs)
        assertThat(execution.errorMessage).isNull()
        assertThat(execution.executedAt).isNotNull()
        assertThat(execution.id).isNull() // ID is not set until persisted
    }

    @Test
    fun `should create QueryExecution with error message for failed status`() {
        // given
        val errorMessage = "Table not found: users"

        // when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.FAILED,
            resultRowCount = 0,
            executionTimeMs = 50L,
            errorMessage = errorMessage
        )

        // then
        assertThat(execution.status).isEqualTo(ExecutionStatus.FAILED)
        assertThat(execution.errorMessage).isEqualTo(errorMessage)
        assertThat(execution.resultRowCount).isEqualTo(0)
    }

    @Test
    fun `should create QueryExecution with TIMEOUT status`() {
        // given & when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.TIMEOUT,
            resultRowCount = 0,
            executionTimeMs = 30000L,
            errorMessage = "Query execution exceeded 30 seconds timeout"
        )

        // then
        assertThat(execution.status).isEqualTo(ExecutionStatus.TIMEOUT)
        assertThat(execution.executionTimeMs).isEqualTo(30000L)
    }

    @Test
    fun `should create QueryExecution with custom executedAt`() {
        // given
        val customTime = LocalDateTime.of(2025, 10, 1, 12, 0, 0)

        // when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.SUCCESS,
            resultRowCount = 50,
            executionTimeMs = 150L,
            errorMessage = null,
            executedAt = customTime
        )

        // then
        assertThat(execution.executedAt).isEqualTo(customTime)
    }

    @Test
    fun `should throw exception when resultRowCount is negative`() {
        // when & then
        assertThatThrownBy {
            QueryExecution(
                generatedSqlQueryId = 1L,
                status = ExecutionStatus.SUCCESS,
                resultRowCount = -1,
                executionTimeMs = 100L,
                errorMessage = null
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Result row count cannot be negative")
    }

    @Test
    fun `should throw exception when executionTimeMs is negative`() {
        // when & then
        assertThatThrownBy {
            QueryExecution(
                generatedSqlQueryId = 1L,
                status = ExecutionStatus.SUCCESS,
                resultRowCount = 10,
                executionTimeMs = -1L,
                errorMessage = null
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Execution time cannot be negative")
    }

    @Test
    fun `should accept zero resultRowCount`() {
        // given & when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.SUCCESS,
            resultRowCount = 0,
            executionTimeMs = 50L,
            errorMessage = null
        )

        // then
        assertThat(execution.resultRowCount).isEqualTo(0)
    }

    @Test
    fun `should accept zero executionTimeMs`() {
        // given & when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.SUCCESS,
            resultRowCount = 10,
            executionTimeMs = 0L,
            errorMessage = null
        )

        // then
        assertThat(execution.executionTimeMs).isEqualTo(0L)
    }

    @Test
    fun `should accept large resultRowCount`() {
        // given
        val largeRowCount = 1000

        // when
        val execution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.SUCCESS,
            resultRowCount = largeRowCount,
            executionTimeMs = 5000L,
            errorMessage = null
        )

        // then
        assertThat(execution.resultRowCount).isEqualTo(largeRowCount)
    }

    @Test
    fun `should support all execution status types`() {
        // given & when
        val successExecution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.SUCCESS,
            resultRowCount = 10,
            executionTimeMs = 100L,
            errorMessage = null
        )

        val failedExecution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.FAILED,
            resultRowCount = 0,
            executionTimeMs = 50L,
            errorMessage = "Error"
        )

        val timeoutExecution = QueryExecution(
            generatedSqlQueryId = 1L,
            status = ExecutionStatus.TIMEOUT,
            resultRowCount = 0,
            executionTimeMs = 30000L,
            errorMessage = "Timeout"
        )

        // then
        assertThat(successExecution.status).isEqualTo(ExecutionStatus.SUCCESS)
        assertThat(failedExecution.status).isEqualTo(ExecutionStatus.FAILED)
        assertThat(timeoutExecution.status).isEqualTo(ExecutionStatus.TIMEOUT)
    }
}
