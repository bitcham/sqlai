package com.sqlai.service.datasource

import com.sqlai.provider.DatabaseIntrospector
import com.sqlai.repository.DatabaseMetadataRepository
import com.sqlai.exception.MetadataSyncException
import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import javax.sql.DataSource

class SyncDatabaseMetadataServiceTest : FunSpec({

    lateinit var introspector: DatabaseIntrospector
    lateinit var metadataRepository: DatabaseMetadataRepository
    lateinit var dataSource: DataSource
    lateinit var service: SyncDatabaseMetadataService

    beforeEach {
        introspector = mockk()
        metadataRepository = mockk()
        dataSource = mockk()
        service = SyncDatabaseMetadataService(introspector, metadataRepository, dataSource)
    }

    afterEach {
        clearAllMocks()
    }

    context("execute - successful metadata sync") {

        test("should sync metadata from datasource and persist it") {
            // given
            val metadata = DatabaseMetadata(schemaName = "test_db").apply {
                val table = TableMetadata(tableName = "users")
                table.columns.add(ColumnMetadata(
                    columnName = "id",
                    dataType = "BIGINT",
                    isNullable = false,
                    isPrimaryKey = true,
                    isForeignKey = false
                ))
                tables.add(table)
            }

            every { introspector.introspect(dataSource) } returns metadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(metadata) } returns metadata

            // when
            val result = service.execute()

            // then
            result.schemaName shouldBe "test_db"
            result.tables.size shouldBe 1
            result.tables.first().tableName shouldBe "users"
            result.tables.first().columns.size shouldBe 1

            verify(exactly = 1) { introspector.introspect(dataSource) }
            verify(exactly = 1) { metadataRepository.deleteAll() }
            verify(exactly = 1) { metadataRepository.save(metadata) }
        }

        test("should delete existing metadata before saving new metadata") {
            // given
            val metadata = DatabaseMetadata(schemaName = "new_db")
            every { introspector.introspect(dataSource) } returns metadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(metadata) } returns metadata

            // when
            service.execute()

            // then
            verifyOrder {
                introspector.introspect(dataSource)
                metadataRepository.deleteAll()
                metadataRepository.save(metadata)
            }
        }

        test("should handle empty database (no tables)") {
            // given
            val emptyMetadata = DatabaseMetadata(schemaName = "empty_db")
            every { introspector.introspect(dataSource) } returns emptyMetadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(emptyMetadata) } returns emptyMetadata

            // when
            val result = service.execute()

            // then
            result.schemaName shouldBe "empty_db"
            result.tables.size shouldBe 0

            verify(exactly = 1) { introspector.introspect(dataSource) }
            verify(exactly = 1) { metadataRepository.save(emptyMetadata) }
        }

        test("should handle database with multiple tables") {
            // given
            val metadata = DatabaseMetadata(schemaName = "multi_table_db").apply {
                val table1 = TableMetadata(tableName = "users")
                table1.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                table1.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))

                val table2 = TableMetadata(tableName = "orders")
                table2.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                table2.columns.add(ColumnMetadata("user_id", "BIGINT", false, false, true, "users", "id"))

                tables.add(table1)
                tables.add(table2)
            }

            every { introspector.introspect(dataSource) } returns metadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(metadata) } returns metadata

            // when
            val result = service.execute()

            // then
            result.tables.size shouldBe 2
            result.tables.sumOf { it.columns.size } shouldBe 4

            verify(exactly = 1) { metadataRepository.save(metadata) }
        }
    }

    context("execute - error handling") {

        test("should propagate MetadataSyncException from introspector") {
            // given
            val errorMessage = "Database connection failed"
            every { introspector.introspect(dataSource) } throws MetadataSyncException(errorMessage)

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                service.execute()
            }

            exception.message shouldContain errorMessage

            verify(exactly = 1) { introspector.introspect(dataSource) }
            verify(exactly = 0) { metadataRepository.deleteAll() }
            verify(exactly = 0) { metadataRepository.save(any()) }
        }

        test("should wrap generic exceptions as MetadataSyncException") {
            // given
            val genericException = RuntimeException("Unexpected error")
            every { introspector.introspect(dataSource) } throws genericException

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                service.execute()
            }

            exception.message shouldContain "Failed to sync database metadata"
            exception.message shouldContain "Unexpected error"

            verify(exactly = 1) { introspector.introspect(dataSource) }
        }

        test("should wrap repository save exceptions as MetadataSyncException") {
            // given
            val metadata = DatabaseMetadata(schemaName = "test_db")
            every { introspector.introspect(dataSource) } returns metadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(metadata) } throws RuntimeException("Database constraint violation")

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                service.execute()
            }

            exception.message shouldContain "Failed to sync database metadata"
            exception.message shouldContain "Database constraint violation"

            verify(exactly = 1) { introspector.introspect(dataSource) }
            verify(exactly = 1) { metadataRepository.deleteAll() }
            verify(exactly = 1) { metadataRepository.save(metadata) }
        }

        test("should handle deleteAll failure gracefully") {
            // given
            val metadata = DatabaseMetadata(schemaName = "test_db")
            every { introspector.introspect(dataSource) } returns metadata
            every { metadataRepository.deleteAll() } throws RuntimeException("Failed to delete old metadata")

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                service.execute()
            }

            exception.message shouldContain "Failed to sync database metadata"

            verify(exactly = 1) { metadataRepository.deleteAll() }
            verify(exactly = 0) { metadataRepository.save(any()) }
        }
    }

    context("Real-world scenarios") {

        test("should sync MySQL database metadata") {
            // given
            val mysqlMetadata = DatabaseMetadata(schemaName = "production_db").apply {
                val usersTable = TableMetadata(tableName = "users")
                usersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                usersTable.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))
                usersTable.columns.add(ColumnMetadata("created_at", "TIMESTAMP", false, false, false))

                val ordersTable = TableMetadata(tableName = "orders")
                ordersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                ordersTable.columns.add(ColumnMetadata("user_id", "BIGINT", false, false, true, "users", "id"))
                ordersTable.columns.add(ColumnMetadata("total_amount", "DECIMAL(10,2)", false, false, false))

                tables.add(usersTable)
                tables.add(ordersTable)
            }

            every { introspector.introspect(dataSource) } returns mysqlMetadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(mysqlMetadata) } returns mysqlMetadata

            // when
            val result = service.execute()

            // then
            result.schemaName shouldBe "production_db"
            result.tables.size shouldBe 2

            val usersTable = result.tables.find { it.tableName == "users" }
            usersTable?.columns?.size shouldBe 3

            val ordersTable = result.tables.find { it.tableName == "orders" }
            ordersTable?.columns?.size shouldBe 3
        }

        test("should handle CSV datasource metadata") {
            // given
            val csvMetadata = DatabaseMetadata(schemaName = "csv_datasource").apply {
                val csvTable = TableMetadata(tableName = "sales_data")
                csvTable.columns.add(ColumnMetadata("date", "DATE", true, false, false))
                csvTable.columns.add(ColumnMetadata("revenue", "DECIMAL", true, false, false))
                csvTable.columns.add(ColumnMetadata("product", "TEXT", true, false, false))

                tables.add(csvTable)
            }

            every { introspector.introspect(dataSource) } returns csvMetadata
            every { metadataRepository.deleteAll() } just Runs
            every { metadataRepository.save(csvMetadata) } returns csvMetadata

            // when
            val result = service.execute()

            // then
            result.schemaName shouldBe "csv_datasource"
            result.tables.size shouldBe 1
            result.tables.first().tableName shouldBe "sales_data"
            result.tables.first().columns.size shouldBe 3
        }
    }
})
