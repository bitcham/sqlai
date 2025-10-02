package com.sqlai.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * HTTP client configuration properties
 *
 * Timeout values prevent DoS attacks and infinite waits when calling external AI APIs
 *
 * @property connectTimeout Connection timeout in milliseconds (default: 5000ms = 5 seconds)
 * @property readTimeout Read timeout in milliseconds (default: 30000ms = 30 seconds)
 * @property connectionRequestTimeout Connection request timeout from pool (default: 3000ms = 3 seconds)
 * @property maxTotalConnections Maximum total connections in the pool (default: 100)
 * @property maxConnectionsPerRoute Maximum connections per route/host (default: 20)
 */
@ConfigurationProperties(prefix = "http.client")
data class HttpClientProperties(
    val connectTimeout: Int = 5000,
    val readTimeout: Int = 30000,
    val connectionRequestTimeout: Int = 3000,
    val maxTotalConnections: Int = 100,
    val maxConnectionsPerRoute: Int = 20
)
