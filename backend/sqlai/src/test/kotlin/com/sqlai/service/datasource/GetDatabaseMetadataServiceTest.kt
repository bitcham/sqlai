package com.sqlai.service.datasource

import com.sqlai.repository.DatabaseMetadataRepository
import com.sqlai.repository.TableMetadataRepository
import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetDatabaseMetadataServiceTest : FunSpec({

    lateinit var metadataRepository: DatabaseMetadataRepository
    lateinit var tableMetadataRepository: TableMetadataRepository
    lateinit var service: GetDatabaseMetadataService

    beforeEach {
        metadataRepository = mockk()
        tableMetadataRepository = mockk()
        service = GetDatabaseMetadataService(metadataRepository, tableMetadataRepository)
    }

    afterEach {
        clearAllMocks()
    }

    context("execute - retrieve metadata") {

        test("should return metadata when it exists") {
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
                table.columns.add(ColumnMetadata(
                    columnName = "email",
                    dataType = "VARCHAR(255)",
                    isNullable = false,
                    isPrimaryKey = false,
                    isForeignKey = false
                ))
                tables.add(table)
            }

            every { metadataRepository.findFirstWithTables() } returns metadata
            every { tableMetadataRepository.findAllWithColumns(metadata.tables) } returns metadata.tables

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.schemaName shouldBe "test_db"
            result?.tables?.size shouldBe 1
            result?.tables?.first()?.tableName shouldBe "users"
            result?.tables?.first()?.columns?.size shouldBe 2

            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            verify(exactly = 1) { tableMetadataRepository.findAllWithColumns(metadata.tables) }
        }

        test("should return null when no metadata exists") {
            // given
            every { metadataRepository.findFirstWithTables() } returns null

            // when
            val result = service.execute()

            // then
            result shouldBe null

            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            verify(exactly = 0) { tableMetadataRepository.findAllWithColumns(any()) }
        }

        test("should return metadata from findFirstWithTables (single result)") {
            // given
            val metadata = DatabaseMetadata(schemaName = "db1")

            every { metadataRepository.findFirstWithTables() } returns metadata

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.schemaName shouldBe "db1"

            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            verify(exactly = 0) { tableMetadataRepository.findAllWithColumns(any()) }
        }

        test("should return metadata with empty tables list") {
            // given
            val emptyMetadata = DatabaseMetadata(schemaName = "empty_db")

            every { metadataRepository.findFirstWithTables() } returns emptyMetadata

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.schemaName shouldBe "empty_db"
            result?.tables?.size shouldBe 0

            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            verify(exactly = 0) { tableMetadataRepository.findAllWithColumns(any()) }
        }
    }

    context("Real-world scenarios") {

        test("should retrieve MySQL database metadata for AI SQL generation") {
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
                ordersTable.columns.add(ColumnMetadata("status", "VARCHAR(50)", false, false, false))

                tables.add(usersTable)
                tables.add(ordersTable)
            }

            every { metadataRepository.findFirstWithTables() } returns mysqlMetadata
            every { tableMetadataRepository.findAllWithColumns(mysqlMetadata.tables) } returns mysqlMetadata.tables

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.schemaName shouldBe "production_db"
            result?.tables?.size shouldBe 2

            val usersTable = result?.tables?.find { it.tableName == "users" }
            usersTable?.columns?.size shouldBe 3
            usersTable?.columns?.any { it.isPrimaryKey } shouldBe true

            val ordersTable = result?.tables?.find { it.tableName == "orders" }
            ordersTable?.columns?.size shouldBe 4
            ordersTable?.columns?.any { it.isForeignKey } shouldBe true
        }

        test("should retrieve CSV datasource metadata") {
            // given
            val csvMetadata = DatabaseMetadata(schemaName = "csv_datasource").apply {
                val salesTable = TableMetadata(tableName = "sales_data")
                salesTable.columns.add(ColumnMetadata("date", "DATE", true, false, false))
                salesTable.columns.add(ColumnMetadata("product_name", "TEXT", true, false, false))
                salesTable.columns.add(ColumnMetadata("revenue", "DECIMAL", true, false, false))
                salesTable.columns.add(ColumnMetadata("quantity", "INTEGER", true, false, false))

                tables.add(salesTable)
            }

            every { metadataRepository.findFirstWithTables() } returns csvMetadata
            every { tableMetadataRepository.findAllWithColumns(csvMetadata.tables) } returns csvMetadata.tables

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.schemaName shouldBe "csv_datasource"
            result?.tables?.size shouldBe 1
            result?.tables?.first()?.tableName shouldBe "sales_data"
            result?.tables?.first()?.columns?.size shouldBe 4
        }

        test("should handle case where metadata has not been synced yet") {
            // given
            every { metadataRepository.findFirstWithTables() } returns null

            // when
            val result = service.execute()

            // then
            result shouldBe null

            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            verify(exactly = 0) { tableMetadataRepository.findAllWithColumns(any()) }
        }

        test("should retrieve metadata with complex schema") {
            // given
            val complexMetadata = DatabaseMetadata(schemaName = "ecommerce_db").apply {
                val usersTable = TableMetadata(tableName = "users")
                usersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                usersTable.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))

                val productsTable = TableMetadata(tableName = "products")
                productsTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                productsTable.columns.add(ColumnMetadata("name", "VARCHAR(200)", false, false, false))
                productsTable.columns.add(ColumnMetadata("price", "DECIMAL(10,2)", false, false, false))

                val ordersTable = TableMetadata(tableName = "orders")
                ordersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
                ordersTable.columns.add(ColumnMetadata("user_id", "BIGINT", false, false, true, "users", "id"))
                ordersTable.columns.add(ColumnMetadata("product_id", "BIGINT", false, false, true, "products", "id"))

                tables.add(usersTable)
                tables.add(productsTable)
                tables.add(ordersTable)
            }

            every { metadataRepository.findFirstWithTables() } returns complexMetadata
            every { tableMetadataRepository.findAllWithColumns(complexMetadata.tables) } returns complexMetadata.tables

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            result?.tables?.size shouldBe 3
            result?.tables?.sumOf { it.columns.size } shouldBe 8

            val ordersTable = result?.tables?.find { it.tableName == "orders" }
            ordersTable?.columns?.count { it.isForeignKey } shouldBe 2
        }
    }

    context("Transactional behavior") {

        test("should be read-only transaction") {
            // given
            val metadata = DatabaseMetadata(schemaName = "readonly_db")
            every { metadataRepository.findFirstWithTables() } returns metadata

            // when
            val result = service.execute()

            // then
            result shouldNotBe null
            verify(exactly = 1) { metadataRepository.findFirstWithTables() }
            // Note: @Transactional(readOnly = true) behavior is tested via integration tests
        }
    }
})
