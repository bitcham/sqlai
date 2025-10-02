package com.sqlai.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

/**
 * Unit test for GlobalExceptionHandler
 *
 * Verifies security features:
 * - API key filtering in error messages
 * - Stack trace removal
 * - Internal path filtering
 */
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `should sanitize Claude API key in error message`() {
        // given
        val exception = IllegalStateException("API call failed with key sk-ant-api03-xyz123")

        // when
        val response = handler.handleIllegalStateException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.message).doesNotContain("sk-ant-api03-xyz123")
        assertThat(response.body?.message).contains("sk-***")
    }

    @Test
    fun `should sanitize OpenAI API key in error message`() {
        // given
        val exception = IllegalStateException("Authentication failed: sk-proj-abc123def456")

        // when
        val response = handler.handleIllegalStateException(exception)

        // then
        assertThat(response.body?.message).doesNotContain("sk-proj-abc123def456")
        assertThat(response.body?.message).contains("sk-***")
    }

    @Test
    fun `should sanitize Gemini API key in error message`() {
        // given
        val exception = IllegalStateException("Invalid API key: AIzaSyABC123DEF456")

        // when
        val response = handler.handleIllegalStateException(exception)

        // then
        assertThat(response.body?.message).doesNotContain("AIzaSyABC123DEF456")
        assertThat(response.body?.message).contains("AIza***")
    }

    @Test
    fun `should remove stack traces from error message`() {
        // given
        val exception = IllegalStateException("Error occurred\n\tat com.sqlai.provider.ClaudeProvider.generateSql")

        // when
        val response = handler.handleIllegalStateException(exception)

        // then
        assertThat(response.body?.message).doesNotContain("\n\tat ")
        assertThat(response.body?.message).doesNotContain("ClaudeProvider")
    }

    @Test
    fun `should handle null error message`() {
        // given
        val exception = IllegalStateException(null as String?)

        // when
        val response = handler.handleIllegalStateException(exception)

        // then
        assertThat(response.body?.message).isEqualTo("An error occurred")
    }

    @Test
    fun `should handle generic exceptions with no sensitive info`() {
        // given
        val exception = RuntimeException("Unexpected error with sk-ant-api03-secret123")

        // when
        val response = handler.handleGenericException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.message).isEqualTo("An unexpected error occurred. Please contact support.")
        assertThat(response.body?.message).doesNotContain("sk-ant-api03-secret123")
    }

    @Test
    fun `should handle UnsafeSqlException correctly`() {
        // given
        val exception = UnsafeSqlException("DROP TABLE detected")

        // when
        val response = handler.handleUnsafeSqlException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.error).isEqualTo("Unsafe SQL")
        assertThat(response.body?.message).contains("DROP TABLE detected")
    }

    @Test
    fun `should handle QueryExecutionException correctly`() {
        // given
        val exception = QueryExecutionException("Query failed: syntax error")

        // when
        val response = handler.handleQueryExecutionException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.error).isEqualTo("Query Execution Error")
        assertThat(response.body?.message).contains("Query failed: syntax error")
    }
}
