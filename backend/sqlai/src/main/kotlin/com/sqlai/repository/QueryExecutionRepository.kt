package com.sqlai.repository

import com.sqlai.domain.query.QueryExecution
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface QueryExecutionRepository : JpaRepository<QueryExecution, Long> {
    @Query("SELECT q FROM QueryExecution q WHERE q.generatedSqlQueryId = :generatedSqlQueryId ORDER BY q.executedAt DESC")
    fun findByGeneratedSqlQueryId(generatedSqlQueryId: Long): List<QueryExecution>
}
