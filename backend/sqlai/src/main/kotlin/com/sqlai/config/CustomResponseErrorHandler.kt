package com.sqlai.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import java.io.IOException

/**
 * Custom error handler for RestTemplate
 *
 * Filters sensitive information from AI Provider API error responses:
 * - Removes API keys from error messages
 * - Provides user-friendly error messages
 * - Logs detailed errors internally for debugging
 *
 * Security principle: Never expose internal details (API keys, URLs, stack traces) to clients
 */
class CustomResponseErrorHandler : DefaultResponseErrorHandler() {

    private val logger = LoggerFactory.getLogger(CustomResponseErrorHandler::class.java)

    @Throws(IOException::class)
    override fun handleError(response: ClientHttpResponse) {
        val statusCode = response.statusCode
        val statusText = response.statusText

        // Log detailed error internally (not exposed to client)
        logger.error("AI Provider API error: status=$statusCode, text=$statusText")

        // Return filtered, user-friendly messages to client
        when (statusCode) {
            HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> {
                throw IllegalStateException("AI provider authentication failed. Please check your configuration.")
            }
            HttpStatus.TOO_MANY_REQUESTS -> {
                throw IllegalStateException("AI provider rate limit exceeded. Please try again later.")
            }
            HttpStatus.BAD_REQUEST -> {
                throw IllegalStateException("Invalid request to AI provider. Please check your input.")
            }
            HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT -> {
                throw IllegalStateException("AI provider service temporarily unavailable. Please try again later.")
            }
            else -> {
                if (statusCode.is5xxServerError) {
                    throw IllegalStateException("AI provider server error. Please contact support.")
                } else {
                    throw IllegalStateException("AI provider request failed with status: ${statusCode.value()}")
                }
            }
        }
    }
}
