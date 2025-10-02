package com.sqlai.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for SQL query execution policies
 * Enables type-safe configuration properties for execution safety features
 */
@Configuration
@EnableConfigurationProperties(ExecutionPolicyProperties::class)
class ExecutionPolicyConfig
