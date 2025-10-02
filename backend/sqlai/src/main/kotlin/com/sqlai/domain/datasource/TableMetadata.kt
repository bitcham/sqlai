package com.sqlai.domain.datasource

import jakarta.persistence.*

/**
 * Entity representing table metadata
 * Represents a single table within a database schema
 * Note: Only TABLE objects are synced, VIEWs are ignored
 */
@Entity
@Table(name = "table_metadata")
class TableMetadata(
    @Column(name = "table_name", nullable = false, length = 255)
    val tableName: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "table_metadata_id", nullable = false)
    val columns: MutableList<ColumnMetadata> = mutableListOf()

    init {
        require(tableName.isNotBlank()) { "Table name must not be blank" }
    }

}