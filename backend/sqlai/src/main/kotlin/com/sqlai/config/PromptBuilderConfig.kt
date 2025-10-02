package com.sqlai.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class to enable PromptBuilderProperties
 */
@Configuration
@EnableConfigurationProperties(PromptBuilderProperties::class)
class PromptBuilderConfig
