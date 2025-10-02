package com.sqlai.domain.datasource

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Entity representing database metadata
 * Contains schema information synced from the configured data source (application.yml)
 * Note: This application supports a single data source configured via Spring Boot properties
 */
@Entity
@Table(name = "database_metadata")
class DatabaseMetadata(
    @Column(name = "schema_name", nullable = false, length = 255)
    val schemaName: String,

    @Column(name = "last_synced_at", nullable = false)
    var lastSyncedAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "metadata_id", nullable = false)
    val tables: MutableList<TableMetadata> = mutableListOf()

    init {
        require(schemaName.isNotBlank()) { "Schema name must not be blank" }
    }

    /**
     * Check if metadata needs to be re-synced
     * Returns true if last sync was more than the specified hours ago
     */
    fun needsResync(thresholdHours: Long = 24): Boolean {
        val hoursSinceLastSync = ChronoUnit.HOURS.between(lastSyncedAt, LocalDateTime.now())
        return hoursSinceLastSync >= thresholdHours
    }

    /**
     * Update sync timestamp
     */
    fun markAsSynced() {
        this.lastSyncedAt = LocalDateTime.now()
    }

}