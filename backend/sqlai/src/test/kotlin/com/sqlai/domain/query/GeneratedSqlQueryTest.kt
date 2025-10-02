package com.sqlai.domain.query

import com.sqlai.domain.ai.AIProviderType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GeneratedSqlQueryTest {

    @Test
    fun `should create GeneratedSqlQuery with valid parameters`() {
        // given
        val naturalLanguageQueryId = 1L
        val sqlStatement = "SELECT * FROM users"
        val explanation = "Retrieves all users"
        val provider = AIProviderType.CLAUDE

        // when
        val sqlQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = naturalLanguageQueryId,
            sqlStatement = sqlStatement,
            explanation = explanation,
            aiProvider = provider
        )

        // then
        assertThat(sqlQuery.naturalLanguageQueryId).isEqualTo(naturalLanguageQueryId)
        assertThat(sqlQuery.sqlStatement).isEqualTo(sqlStatement)
        assertThat(sqlQuery.explanation).isEqualTo(explanation)
        assertThat(sqlQuery.aiProvider).isEqualTo(provider)
        assertThat(sqlQuery.generatedAt).isNotNull()
        assertThat(sqlQuery.id).isNull() // ID is not set until persisted
    }

    @Test
    fun `should create GeneratedSqlQuery with null explanation`() {
        // given
        val naturalLanguageQueryId = 1L
        val sqlStatement = "SELECT * FROM users"

        // when
        val sqlQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = naturalLanguageQueryId,
            sqlStatement = sqlStatement,
            explanation = null,
            aiProvider = AIProviderType.OPENAI
        )

        // then
        assertThat(sqlQuery.explanation).isNull()
    }

    @Test
    fun `should create GeneratedSqlQuery with custom generatedAt`() {
        // given
        val customTime = LocalDateTime.of(2025, 10, 1, 12, 0, 0)

        // when
        val sqlQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = 1L,
            sqlStatement = "SELECT * FROM users",
            explanation = null,
            aiProvider = AIProviderType.GEMINI,
            generatedAt = customTime
        )

        // then
        assertThat(sqlQuery.generatedAt).isEqualTo(customTime)
    }

    @Test
    fun `should throw exception when sqlStatement is blank`() {
        // when & then
        assertThatThrownBy {
            GeneratedSqlQuery(
                naturalLanguageQueryId = 1L,
                sqlStatement = "",
                explanation = null,
                aiProvider = AIProviderType.CLAUDE
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("SQL statement cannot be blank")
    }

    @Test
    fun `should throw exception when sqlStatement is whitespace only`() {
        // when & then
        assertThatThrownBy {
            GeneratedSqlQuery(
                naturalLanguageQueryId = 1L,
                sqlStatement = "   ",
                explanation = null,
                aiProvider = AIProviderType.CLAUDE
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("SQL statement cannot be blank")
    }

    @Test
    fun `should accept complex SQL statement`() {
        // given
        val complexSql = """
            SELECT u.id, u.name, COUNT(o.id) as order_count
            FROM users u
            LEFT JOIN orders o ON u.id = o.user_id
            WHERE u.created_at >= '2025-01-01'
            GROUP BY u.id, u.name
            ORDER BY order_count DESC
        """.trimIndent()

        // when
        val sqlQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = 1L,
            sqlStatement = complexSql,
            explanation = "Get user order counts",
            aiProvider = AIProviderType.CLAUDE
        )

        // then
        assertThat(sqlQuery.sqlStatement).isEqualTo(complexSql)
    }

    @Test
    fun `should support all AI provider types`() {
        // given & when
        val claudeQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = 1L,
            sqlStatement = "SELECT 1",
            explanation = null,
            aiProvider = AIProviderType.CLAUDE
        )

        val openaiQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = 1L,
            sqlStatement = "SELECT 1",
            explanation = null,
            aiProvider = AIProviderType.OPENAI
        )

        val geminiQuery = GeneratedSqlQuery(
            naturalLanguageQueryId = 1L,
            sqlStatement = "SELECT 1",
            explanation = null,
            aiProvider = AIProviderType.GEMINI
        )

        // then
        assertThat(claudeQuery.aiProvider).isEqualTo(AIProviderType.CLAUDE)
        assertThat(openaiQuery.aiProvider).isEqualTo(AIProviderType.OPENAI)
        assertThat(geminiQuery.aiProvider).isEqualTo(AIProviderType.GEMINI)
    }
}
