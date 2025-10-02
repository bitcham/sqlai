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
 * OpenAI provider implementation
 * Uses OpenAI's GPT API for SQL generation
 *
 * API Documentation: https://platform.openai.com/docs/api-reference/chat/create
 */
@Component
@ConditionalOnProperty(prefix = "ai.providers.openai", name = ["api-key"])
class OpenAIProvider(
    private val properties: AIProviderProperties,
    @Qualifier("aiProviderRestTemplate") private val restTemplate: RestTemplate
) : AIProvider {

    private val logger = LoggerFactory.getLogger(OpenAIProvider::class.java)

    private val apiUrl = "https://api.openai.com/v1/chat/completions"
    private val config = properties.providers.openai

    override fun generateSql(prompt: String): SqlGenerationResult {
        logger.info("Generating SQL using OpenAI (model: ${config.model})")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(config.apiKey)
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
                ?: throw IllegalStateException("Empty response from OpenAI API")

            val sqlStatement = extractSqlFromResponse(response)
            val explanation = extractExplanationFromResponse(response)

            logger.info("Successfully generated SQL using OpenAI")

            SqlGenerationResult(
                sqlStatement = sqlStatement,
                explanation = explanation,
                provider = AIProviderType.OPENAI
            )
        } catch (e: Exception) {
            logger.error("Failed to generate SQL using OpenAI", e)
            throw IllegalStateException("OpenAI API call failed: ${e.message}", e)
        }
    }

    override fun getProviderType(): AIProviderType = AIProviderType.OPENAI

    override fun isAvailable(): Boolean {
        return config.apiKey.isNotBlank()
    }

    private fun extractSqlFromResponse(response: Map<*, *>): String {
        @Suppress("UNCHECKED_CAST")
        val choices = response["choices"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("Invalid response format from OpenAI")

        val firstChoice = choices.firstOrNull()
            ?: throw IllegalStateException("No choices in OpenAI response")

        val message = firstChoice["message"] as? Map<String, Any>
            ?: throw IllegalStateException("No message in OpenAI response")

        val fullText = message["content"] as? String
            ?: throw IllegalStateException("No content in OpenAI response")

        // Extract SQL from response
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
        val choices = response["choices"] as? List<Map<String, Any>> ?: return null
        val firstChoice = choices.firstOrNull() ?: return null
        val message = firstChoice["message"] as? Map<String, Any> ?: return null
        val fullText = message["content"] as? String ?: return null

        return if (fullText.contains("EXPLANATION:", ignoreCase = true)) {
            fullText.substringAfter("EXPLANATION:", "").trim()
        } else null
    }
}
