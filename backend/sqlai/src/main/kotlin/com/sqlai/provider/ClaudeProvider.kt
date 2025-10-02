package com.sqlai.provider

import com.sqlai.config.AIProviderProperties
import com.sqlai.provider.AIProvider
import com.sqlai.domain.ai.AIProviderType
import com.sqlai.domain.ai.SqlGenerationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Claude AI provider implementation
 * Uses Anthropic's Claude API for SQL generation
 *
 * API Documentation: https://docs.anthropic.com/claude/reference
 */
@Component
@ConditionalOnProperty(prefix = "ai.providers.claude", name = ["api-key"])
class ClaudeProvider(
    private val properties: AIProviderProperties,
    @Qualifier("aiProviderRestTemplate") private val restTemplate: RestTemplate
) : AIProvider {

    private val logger = LoggerFactory.getLogger(ClaudeProvider::class.java)

    private val apiUrl = "https://api.anthropic.com/v1/messages"
    private val config = properties.providers.claude

    override fun generateSql(prompt: String): SqlGenerationResult {
        logger.info("Generating SQL using Claude (model: ${config.model})")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-api-key", config.apiKey)
            set("anthropic-version", "2023-06-01")
        }

        val requestBody = mapOf(
            "model" to config.model,
            "max_tokens" to config.maxTokens,
            "temperature" to config.temperature,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            )
        )

        val request = HttpEntity(requestBody, headers)

        return try {
            val response = restTemplate.postForObject(apiUrl, request, Map::class.java)
                ?: throw IllegalStateException("Empty response from Claude API")

            val sqlStatement = extractSqlFromResponse(response)
            val explanation = extractExplanationFromResponse(response)

            logger.info("Successfully generated SQL using Claude")

            SqlGenerationResult(
                sqlStatement = sqlStatement,
                explanation = explanation,
                provider = AIProviderType.CLAUDE
            )
        } catch (e: Exception) {
            logger.error("Failed to generate SQL using Claude", e)
            throw IllegalStateException("Claude API call failed: ${e.message}", e)
        }
    }

    override fun getProviderType(): AIProviderType = AIProviderType.CLAUDE

    override fun isAvailable(): Boolean {
        return config.apiKey.isNotBlank()
    }

    private fun extractSqlFromResponse(response: Map<*, *>): String {
        @Suppress("UNCHECKED_CAST")
        val content = response["content"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("Invalid response format from Claude")

        val textContent = content.firstOrNull { it["type"] == "text" }
            ?: throw IllegalStateException("No text content in Claude response")

        val fullText = textContent["text"] as? String
            ?: throw IllegalStateException("No text field in Claude response")

        // Extract SQL from response
        // Expected format: "SQL: <query>" or just the SQL query
        return when {
            fullText.contains("SQL:", ignoreCase = true) -> {
                fullText.substringAfter("SQL:", "").substringBefore("EXPLANATION:", fullText).trim()
            }
            fullText.contains("```sql") -> {
                fullText.substringAfter("```sql").substringBefore("```").trim()
            }
            else -> fullText.trim()
        }
    }

    private fun extractExplanationFromResponse(response: Map<*, *>): String? {
        @Suppress("UNCHECKED_CAST")
        val content = response["content"] as? List<Map<String, Any>> ?: return null
        val textContent = content.firstOrNull { it["type"] == "text" } ?: return null
        val fullText = textContent["text"] as? String ?: return null

        return if (fullText.contains("EXPLANATION:", ignoreCase = true)) {
            fullText.substringAfter("EXPLANATION:", "").trim()
        } else null
    }
}
