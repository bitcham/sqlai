package com.sqlai.dto

/**
 * Metadata information about a result column
 */
data class ColumnInfo(
    /**
     * Column name
     */
    val name: String,

    /**
     * SQL data type (e.g., BIGINT, VARCHAR, DATE)
     */
    val type: String
)
