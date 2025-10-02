package com.sqlai.dto

import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for SQL query generation
 */
data class GenerateQueryRequest(
    @field:NotBlank(message = "Question must not be blank")
    val question: String
)
