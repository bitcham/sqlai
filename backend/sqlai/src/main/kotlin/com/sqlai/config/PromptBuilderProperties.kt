package com.sqlai.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for prompt building
 */
@ConfigurationProperties(prefix = "prompt")
data class PromptBuilderProperties(
    /**
     * Database type for prompt context (e.g., H2, POSTGRESQL, MYSQL)
     */
    val databaseType: String = "H2"
)
