package com.sqlai.repository

import com.sqlai.domain.query.NaturalLanguageQuery
import org.springframework.data.jpa.repository.JpaRepository

interface NaturalLanguageQueryRepository : JpaRepository<NaturalLanguageQuery, Long>
