package com.sqlai.config

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse

/**
 * Unit test for CustomResponseErrorHandler
 *
 * Verifies that error messages are filtered to prevent sensitive information exposure:
 * - 401 Unauthorized: Generic authentication message
 * - 429 Rate Limit: Rate limit message
 * - 500 Server Error: Generic server error message
 */
class CustomResponseErrorHandlerTest {

    private val errorHandler = CustomResponseErrorHandler()

    @Test
    fun `should throw filtered message for 401 Unauthorized`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.UNAUTHORIZED
        every { response.statusText } returns "Unauthorized"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("authentication failed")
        assertThat(exception.message).doesNotContain("API key")
        assertThat(exception.message).doesNotContain("sk-")
    }

    @Test
    fun `should throw filtered message for 403 Forbidden`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.FORBIDDEN
        every { response.statusText } returns "Forbidden"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("authentication failed")
    }

    @Test
    fun `should throw filtered message for 429 Rate Limit`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.TOO_MANY_REQUESTS
        every { response.statusText } returns "Too Many Requests"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("rate limit exceeded")
        assertThat(exception.message).contains("try again later")
    }

    @Test
    fun `should throw filtered message for 400 Bad Request`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.BAD_REQUEST
        every { response.statusText } returns "Bad Request"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("Invalid request")
    }

    @Test
    fun `should throw filtered message for 500 Internal Server Error`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.INTERNAL_SERVER_ERROR
        every { response.statusText } returns "Internal Server Error"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("server error")
        assertThat(exception.message).doesNotContain("Internal Server Error")
    }

    @Test
    fun `should throw filtered message for 503 Service Unavailable`() {
        // given
        val response = mockk<ClientHttpResponse>()
        every { response.statusCode } returns HttpStatus.SERVICE_UNAVAILABLE
        every { response.statusText } returns "Service Unavailable"

        // when & then
        val exception = assertThrows<IllegalStateException> {
            errorHandler.handleError(response)
        }

        assertThat(exception.message).contains("temporarily unavailable")
    }
}
