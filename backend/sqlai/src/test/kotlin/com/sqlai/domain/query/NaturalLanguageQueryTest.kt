package com.sqlai.domain.query

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NaturalLanguageQueryTest {

    @Test
    fun `should create NaturalLanguageQuery with valid question`() {
        // given
        val question = "Show all users"

        // when
        val nlQuery = NaturalLanguageQuery(question = question)

        // then
        assertThat(nlQuery.question).isEqualTo(question)
        assertThat(nlQuery.createdAt).isNotNull()
        assertThat(nlQuery.id).isNull() // ID is not set until persisted
    }

    @Test
    fun `should create NaturalLanguageQuery with custom createdAt`() {
        // given
        val question = "Show all users"
        val customTime = LocalDateTime.of(2025, 10, 1, 12, 0, 0)

        // when
        val nlQuery = NaturalLanguageQuery(
            question = question,
            createdAt = customTime
        )

        // then
        assertThat(nlQuery.createdAt).isEqualTo(customTime)
    }

    @Test
    fun `should throw exception when question is blank`() {
        // when & then
        assertThatThrownBy {
            NaturalLanguageQuery(question = "")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Question cannot be blank")
    }

    @Test
    fun `should throw exception when question is whitespace only`() {
        // when & then
        assertThatThrownBy {
            NaturalLanguageQuery(question = "   ")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Question cannot be blank")
    }

    @Test
    fun `should accept question with multiple lines`() {
        // given
        val question = """
            Show all users
            who registered this month
        """.trimIndent()

        // when
        val nlQuery = NaturalLanguageQuery(question = question)

        // then
        assertThat(nlQuery.question).isEqualTo(question)
    }
}
