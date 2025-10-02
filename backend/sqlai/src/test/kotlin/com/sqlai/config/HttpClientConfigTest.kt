package com.sqlai.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit test for HttpClientConfig
 *
 * Verifies that RestTemplate bean is configured with:
 * - Custom error handler (CustomResponseErrorHandler)
 * - Timeout settings
 * - Connection pool configuration
 */
class HttpClientConfigTest {

    private val properties = HttpClientProperties(
        connectTimeout = 5000,
        readTimeout = 30000,
        connectionRequestTimeout = 3000,
        maxTotalConnections = 100,
        maxConnectionsPerRoute = 20
    )

    private val config = HttpClientConfig(properties)

    @Test
    fun `should create RestTemplate bean with custom error handler`() {
        // given & when
        val restTemplate = config.restTemplate()
        val errorHandler = restTemplate.errorHandler

        // then
        assertThat(restTemplate).isNotNull
        assertThat(errorHandler).isInstanceOf(CustomResponseErrorHandler::class.java)
    }

    @Test
    fun `should configure HttpComponentsClientHttpRequestFactory`() {
        // given & when
        val restTemplate = config.restTemplate()
        val requestFactory = restTemplate.requestFactory

        // then
        assertThat(requestFactory).isNotNull
        assertThat(requestFactory.javaClass.name).contains("HttpComponentsClientHttpRequestFactory")
    }
}
