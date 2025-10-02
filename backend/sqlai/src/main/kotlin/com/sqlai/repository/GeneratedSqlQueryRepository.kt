package com.sqlai.repository

import com.sqlai.domain.query.GeneratedSqlQuery
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GeneratedSqlQueryRepository : JpaRepository<GeneratedSqlQuery, Long> {
    @Query("SELECT g FROM GeneratedSqlQuery g WHERE g.naturalLanguageQueryId = :naturalLanguageQueryId ORDER BY g.generatedAt DESC")
    fun findByNaturalLanguageQueryId(naturalLanguageQueryId: Long): List<GeneratedSqlQuery>
}
