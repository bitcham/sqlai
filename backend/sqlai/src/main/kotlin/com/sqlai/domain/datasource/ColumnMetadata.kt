package com.sqlai.domain.datasource

import jakarta.persistence.*

/**
 * Entity representing column metadata
 * Contains detailed information about a column including relationships
 */
@Entity
@Table(name = "column_metadata")
class ColumnMetadata(
    @Column(name = "column_name", nullable = false, length = 255)
    val columnName: String,

    @Column(name = "data_type", nullable = false, length = 100)
    val dataType: String,

    @Column(name = "is_nullable", nullable = false)
    val isNullable: Boolean = true,

    @Column(name = "is_primary_key", nullable = false)
    val isPrimaryKey: Boolean = false,

    @Column(name = "is_foreign_key", nullable = false)
    val isForeignKey: Boolean = false,

    @Column(name = "referenced_table", length = 255)
    val referencedTable: String? = null,

    @Column(name = "referenced_column", length = 255)
    val referencedColumn: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
    init {
        require(columnName.isNotBlank()) { "Column name must not be blank" }
        require(dataType.isNotBlank()) { "Data type must not be blank" }

        // Foreign key validation
        if (isForeignKey) {
            require(!referencedTable.isNullOrBlank()) {
                "Foreign key column must have referencedTable"
            }
            require(!referencedColumn.isNullOrBlank()) {
                "Foreign key column must have referencedColumn"
            }
        }
    }
    fun hasForeignKeyRelation(): Boolean {
        return isForeignKey &&
                !referencedTable.isNullOrBlank() &&
                !referencedColumn.isNullOrBlank()
    }
    fun getForeignKeyReference(): String? {
        return if (hasForeignKeyRelation()) {
            "$referencedTable.$referencedColumn"
        } else {
            null
        }
    }

}