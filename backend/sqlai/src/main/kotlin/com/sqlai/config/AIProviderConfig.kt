package com.sqlai.config

import com.sqlai.provider.AIProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * Configuration for AI providers
 * Enables type-safe configuration properties and selects the active AI provider
 *
 * SECURITY: API keys are loaded from environment variables via application.yml
 * NEVER hardcode API keys in this file
 */
@Configuration
@EnableConfigurationProperties(AIProviderProperties::class)
class AIProviderConfig {

    private val logger = LoggerFactory.getLogger(AIProviderConfig::class.java)

    /**
     * Provides the active AI provider based on configuration
     * Selects the provider specified in application.yml
     *
     * Note: Provider implementations will be registered as beans separately
     * This method selects which one to use as the primary provider
     */
    @Bean
    @Primary
    fun activeAIProvider(
        properties: AIProviderProperties,
        providers: List<AIProvider>
    ): AIProvider {
        val providerType = properties.provider
        logger.info("Configured AI provider: $providerType")

        // Find the provider matching the configured type
        val selectedProvider = providers.firstOrNull { it.getProviderType() == providerType }
            ?: providers.firstOrNull()
            ?: throw IllegalStateException("No AI providers available. Check configuration.")

        logger.info("Selected AI provider: ${selectedProvider.getProviderType()}")

        // Check if provider is available
        if (!selectedProvider.isAvailable()) {
            logger.warn("Selected provider ${selectedProvider.getProviderType()} is not available (check API key)")
        }

        return selectedProvider
    }

    /**
     * Validate that at least one AI provider is properly configured
     */
    @Bean
    fun aiProviderValidator(properties: AIProviderProperties): AIProviderValidator {
        return AIProviderValidator(properties)
    }
}

/**
 * Validator to ensure at least one AI provider has API key configured
 */
class AIProviderValidator(private val properties: AIProviderProperties) {

    private val logger = LoggerFactory.getLogger(AIProviderValidator::class.java)

    init {
        validate()
    }

    private fun validate() {
        val claudeConfigured = properties.providers.claude.apiKey.isNotBlank()
        val openaiConfigured = properties.providers.openai.apiKey.isNotBlank()
        val geminiConfigured = properties.providers.gemini.apiKey.isNotBlank()

        if (!claudeConfigured && !openaiConfigured && !geminiConfigured) {
            logger.error("No AI provider API keys configured. Set CLAUDE_API_KEY, OPENAI_API_KEY, or GEMINI_API_KEY environment variables")
            throw IllegalStateException(
                "No AI provider API keys configured. Please set at least one of: " +
                        "CLAUDE_API_KEY, OPENAI_API_KEY, GEMINI_API_KEY"
            )
        }

        logger.info("AI Provider configuration validated successfully")
        logger.info("  - Claude: ${if (claudeConfigured) "Configured" else "Not configured"}")
        logger.info("  - OpenAI: ${if (openaiConfigured) "Configured" else "Not configured"}")
        logger.info("  - Gemini: ${if (geminiConfigured) "Configured" else "Not configured"}")
    }
}
