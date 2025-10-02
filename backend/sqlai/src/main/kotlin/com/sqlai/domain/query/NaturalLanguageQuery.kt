package com.sqlai.domain.query

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Represents a user's natural language question.
 * This is the entry point of the query execution flow.
 */
@Entity
@Table(name = "natural_language_queries")
class NaturalLanguageQuery(
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    val question: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    init {
        require(question.isNotBlank()) { "Question cannot be blank" }
    }
}
