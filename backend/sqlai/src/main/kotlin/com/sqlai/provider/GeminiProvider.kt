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
 * Google Gemini provider implementation
 * Uses Google's Gemini API for SQL generation
 *
 * API Documentation: https://ai.google.dev/api/rest
 */
@Component
@ConditionalOnProperty(prefix = "ai.providers.gemini", name = ["api-key"])
class GeminiProvider(
    private val properties: AIProviderProperties,
    @Qualifier("aiProviderRestTemplate") private val restTemplate: RestTemplate
) : AIProvider {

    private val logger = LoggerFactory.getLogger(GeminiProvider::class.java)

    private val config = properties.providers.gemini

    override fun generateSql(prompt: String): SqlGenerationResult {
        logger.info("Generating SQL using Gemini (model: ${config.model})")

        val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=${config.apiKey}"

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to config.temperature,
                "maxOutputTokens" to config.maxTokens
            )
        )

        val request = HttpEntity(requestBody, headers)

        return try {
            val response = restTemplate.postForObject(apiUrl, request, Map::class.java)
                ?: throw IllegalStateException("Empty response from Gemini API")

            val sqlStatement = extractSqlFromResponse(response)
            val explanation = extractExplanationFromResponse(response)

            logger.info("Successfully generated SQL using Gemini")

            SqlGenerationResult(
                sqlStatement = sqlStatement,
                explanation = explanation,
                provider = AIProviderType.GEMINI
            )
        } catch (e: Exception) {
            logger.error("Failed to generate SQL using Gemini", e)
            throw IllegalStateException("Gemini API call failed: ${e.message}", e)
        }
    }

    override fun getProviderType(): AIProviderType = AIProviderType.GEMINI

    override fun isAvailable(): Boolean {
        return config.apiKey.isNotBlank()
    }

    private fun extractSqlFromResponse(response: Map<*, *>): String {
        @Suppress("UNCHECKED_CAST")
        val candidates = response["candidates"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("Invalid response format from Gemini")

        val firstCandidate = candidates.firstOrNull()
            ?: throw IllegalStateException("No candidates in Gemini response")

        val content = firstCandidate["content"] as? Map<String, Any>
            ?: throw IllegalStateException("No content in Gemini response")

        val parts = content["parts"] as? List<Map<String, Any>>
            ?: throw IllegalStateException("No parts in Gemini response")

        val firstPart = parts.firstOrNull()
            ?: throw IllegalStateException("No parts in Gemini response")

        val fullText = firstPart["text"] as? String
            ?: throw IllegalStateException("No text in Gemini response")

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
        val candidates = response["candidates"] as? List<Map<String, Any>> ?: return null
        val firstCandidate = candidates.firstOrNull() ?: return null
        val content = firstCandidate["content"] as? Map<String, Any> ?: return null
        val parts = content["parts"] as? List<Map<String, Any>> ?: return null
        val firstPart = parts.firstOrNull() ?: return null
        val fullText = firstPart["text"] as? String ?: return null

        return if (fullText.contains("EXPLANATION:", ignoreCase = true)) {
            fullText.substringAfter("EXPLANATION:", "").trim()
        } else null
    }
}
