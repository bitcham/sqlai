package com.sqlai.provider

import com.sqlai.domain.ai.AIProviderType
import com.sqlai.domain.ai.SqlGenerationResult

/**
 * Interface for AI provider integration
 * Implementations: ClaudeProvider, OpenAIProvider, GeminiProvider
 */
interface AIProvider {
    /**
     * Generate SQL query from natural language prompt
     * @param prompt Full prompt including schema context and user question
     * @return SQL generation result with query, explanation, and metadata
     */
    fun generateSql(prompt: String): SqlGenerationResult

    /**
     * Get the type of this AI provider
     */
    fun getProviderType(): AIProviderType

    /**
     * Check if this provider is available (API key configured, service reachable)
     */
    fun isAvailable(): Boolean
}
