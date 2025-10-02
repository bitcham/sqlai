package com.sqlai.config

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.Timeout
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * HTTP client configuration for external API calls (AI Providers)
 *
 * Security features:
 * - Connection timeout: Prevents DoS attacks and infinite waiting
 * - Read timeout: Limits maximum wait time for API responses
 * - Connection pool: Manages resources efficiently
 * - Custom error handler: Filters sensitive information from error messages
 *
 * Configuration source: application.yml (http.client.*)
 */
@Configuration
@EnableConfigurationProperties(HttpClientProperties::class)
class HttpClientConfig(
    private val properties: HttpClientProperties
) {

    /**
     * Configure RestTemplate with security timeouts and error handling
     *
     * This bean is injected into all AI Provider implementations:
     * - ClaudeProvider
     * - OpenAIProvider
     * - GeminiProvider
     *
     * Bean name "aiProviderRestTemplate" prevents conflicts with other RestTemplate beans
     */
    @Bean("aiProviderRestTemplate")
    fun restTemplate(): RestTemplate {
        // Connection pool configuration
        val connectionManager = PoolingHttpClientConnectionManager().apply {
            maxTotal = properties.maxTotalConnections
            defaultMaxPerRoute = properties.maxConnectionsPerRoute
        }

        // Timeout configuration (security: prevent infinite waits)
        val requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(properties.connectTimeout.toLong()))
            .setResponseTimeout(Timeout.ofMilliseconds(properties.readTimeout.toLong()))
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.connectionRequestTimeout.toLong()))
            .build()

        // Build HttpClient with connection pool and timeouts
        val httpClient: HttpClient = HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build()

        // Create HttpComponentsClientHttpRequestFactory
        val factory = HttpComponentsClientHttpRequestFactory(httpClient)

        // Create RestTemplate with custom error handler
        return RestTemplate(factory).apply {
            errorHandler = CustomResponseErrorHandler()
        }
    }
}
