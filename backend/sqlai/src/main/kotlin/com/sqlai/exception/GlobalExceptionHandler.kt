package com.sqlai.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLTimeoutException
import java.time.LocalDateTime

/**
 * Global exception handler for REST API
 * Provides consistent error responses across all controllers
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handle validation errors (e.g., @Valid failures)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }

        logger.warn("Validation error: $errors")

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation Error",
                    message = errors.joinToString(", "),
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Illegal argument: ${ex.message}")

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Bad Request",
                    message = ex.message ?: "Invalid request",
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle illegal state exceptions (e.g., metadata not available, AI API failures)
     *
     * Security: Filters sensitive information from error messages (API keys, stack traces)
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        // Log detailed error internally (not exposed to client)
        logger.error("Illegal state: ${ex.message}", ex)

        // Return sanitized message to client
        val sanitizedMessage = sanitizeErrorMessage(ex.message)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Service Error",
                    message = sanitizedMessage,
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle SQL timeout exceptions
     */
    @ExceptionHandler(SQLTimeoutException::class)
    fun handleSQLTimeoutException(ex: SQLTimeoutException): ResponseEntity<ErrorResponse> {
        logger.warn("Query timeout: ${ex.message}")

        return ResponseEntity
            .status(HttpStatus.REQUEST_TIMEOUT)
            .body(
                ErrorResponse(
                    status = HttpStatus.REQUEST_TIMEOUT.value(),
                    error = "Query Timeout",
                    message = "Query execution exceeded 30 seconds timeout",
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle unsafe SQL exceptions
     */
    @ExceptionHandler(UnsafeSqlException::class)
    fun handleUnsafeSqlException(ex: UnsafeSqlException): ResponseEntity<ErrorResponse> {
        logger.warn("Unsafe SQL detected: ${ex.message}")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Unsafe SQL",
                    message = ex.message ?: "Query contains unsafe operations",
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle query execution exceptions
     */
    @ExceptionHandler(QueryExecutionException::class)
    fun handleQueryExecutionException(ex: QueryExecutionException): ResponseEntity<ErrorResponse> {
        logger.error("Query execution failed: ${ex.message}", ex)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Query Execution Error",
                    message = ex.message ?: "Failed to execute query",
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Handle all other exceptions
     *
     * Security: Never expose internal details to clients
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        // Log detailed error internally (not exposed to client)
        logger.error("Unexpected error: ${ex.message}", ex)

        // Generic message for unexpected errors (no sensitive information)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal Server Error",
                    message = "An unexpected error occurred. Please contact support.",
                    timestamp = LocalDateTime.now()
                )
            )
    }

    /**
     * Remove sensitive information from error messages
     *
     * Filters out:
     * - API keys (Claude: sk-*, OpenAI: sk-*, Gemini: AIza*)
     * - Stack traces (lines starting with "at ")
     * - Internal URLs and paths
     *
     * Security principle: Never expose internal implementation details to clients
     */
    private fun sanitizeErrorMessage(message: String?): String {
        if (message == null) return "An error occurred"

        var sanitized = message

        // Remove API key patterns
        sanitized = sanitized.replace(Regex("(api[_-]?key[\\s=:]+)[\\w-]+", RegexOption.IGNORE_CASE), "$1***")
        sanitized = sanitized.replace(Regex("sk-[a-zA-Z0-9-]+"), "sk-***")  // Claude/OpenAI keys
        sanitized = sanitized.replace(Regex("AIza[a-zA-Z0-9-]+"), "AIza***")  // Gemini keys

        // Remove stack traces (everything after "\n\tat ")
        sanitized = sanitized.substringBefore("\n\tat ")

        // Remove internal file paths
        sanitized = sanitized.replace(Regex("at com\\.sqlai\\.[\\w.]+"), "")

        return sanitized.ifEmpty { "Service error occurred" }
    }
}

/**
 * Standard error response format
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: LocalDateTime
)
