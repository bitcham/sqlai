package com.sqlai.config

import com.sqlai.domain.ai.AIProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Type-safe configuration properties for AI providers
 * API keys are loaded from environment variables (NEVER hardcoded)
 *
 * Example application.yml:
 * ```
 * ai:
 *   provider: CLAUDE
 *   providers:
 *     claude:
 *       api-key: ${CLAUDE_API_KEY}
 *       model: claude-3-5-sonnet-20241022
 *       max-tokens: 2000
 * ```
 */
@ConfigurationProperties(prefix = "ai")
data class AIProviderProperties(
    val provider: AIProviderType,
    val providers: ProvidersConfig = ProvidersConfig()
)

data class ProvidersConfig(
    val claude: ProviderConfig = ProviderConfig(),
    val openai: ProviderConfig = ProviderConfig(),
    val gemini: ProviderConfig = ProviderConfig()
)

data class ProviderConfig(
    val apiKey: String = "",
    val model: String = "",
    val maxTokens: Int = 2000,
    val temperature: Double = 0.0
)
