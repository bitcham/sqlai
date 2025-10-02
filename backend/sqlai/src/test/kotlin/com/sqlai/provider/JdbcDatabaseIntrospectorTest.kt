package com.sqlai.provider

import com.sqlai.exception.MetadataSyncException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import javax.sql.DataSource

class JdbcDatabaseIntrospectorTest : FunSpec({

    var dataSource: DataSource? = null
    var connection: Connection? = null
    var metaData: DatabaseMetaData? = null
    var introspector: JdbcDatabaseIntrospector? = null

    beforeTest {
        dataSource = mockk()
        connection = mockk()
        metaData = mockk()
        introspector = JdbcDatabaseIntrospector()

        every { dataSource!!.connection } returns connection!!
        every { connection!!.metaData } returns metaData!!
        every { connection!!.close() } just Runs
    }

    afterTest {
        clearAllMocks()
    }

    context("introspect - successful metadata extraction") {

        test("should extract database metadata with single table") {
            // given
            val schema = "test_db"
            val tablesResultSet = mockk<ResultSet>()
            val columnsResultSet = mockk<ResultSet>()
            val primaryKeysResultSet = mockk<ResultSet>()
            val foreignKeysResultSet = mockk<ResultSet>()

            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema

            // Mock getTables() - single table "users"
            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } returns tablesResultSet
            every { tablesResultSet.next() } returnsMany listOf(true, false)
            every { tablesResultSet.getString("TABLE_NAME") } returns "users"

            // Mock getColumns() - id, email columns
            every { metaData!!.getColumns(null, schema, "users", "%") } returns columnsResultSet
            every { columnsResultSet.next() } returnsMany listOf(true, true, false)
            every { columnsResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "email")
            every { columnsResultSet.getString("TYPE_NAME") } returnsMany listOf("BIGINT", "VARCHAR")
            every { columnsResultSet.getString("IS_NULLABLE") } returnsMany listOf("NO", "NO") // NOT NULL

            // Mock getPrimaryKeys() - id is PK
            every { metaData!!.getPrimaryKeys(null, schema, "users") } returns primaryKeysResultSet
            every { primaryKeysResultSet.next() } returnsMany listOf(true, false)
            every { primaryKeysResultSet.getString("COLUMN_NAME") } returns "id"

            // Mock getImportedKeys() - no foreign keys
            every { metaData!!.getImportedKeys(null, schema, "users") } returns foreignKeysResultSet
            every { foreignKeysResultSet.next() } returns false

            every { tablesResultSet.close() } just Runs
            every { columnsResultSet.close() } just Runs
            every { primaryKeysResultSet.close() } just Runs
            every { foreignKeysResultSet.close() } just Runs

            // when
            val result = introspector!!.introspect(dataSource!!)

            // then
            result shouldNotBe null
            result.schemaName shouldBe schema
            result.tables.size shouldBe 1

            val usersTable = result.tables.first()
            usersTable.tableName shouldBe "users"
            usersTable.columns.size shouldBe 2

            val idColumn = usersTable.columns.find { it.columnName == "id" }
            idColumn shouldNotBe null
            idColumn?.isPrimaryKey shouldBe true
            idColumn?.isForeignKey shouldBe false

            val emailColumn = usersTable.columns.find { it.columnName == "email" }
            emailColumn shouldNotBe null
            emailColumn?.isPrimaryKey shouldBe false

            verify(exactly = 1) { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) }
            verify(exactly = 1) { connection!!.close() }
        }

        test("should extract metadata with multiple tables") {
            // given
            val schema = "test_db"
            val tablesResultSet = mockk<ResultSet>()
            val usersColumnsResultSet = mockk<ResultSet>()
            val ordersColumnsResultSet = mockk<ResultSet>()
            val usersPkResultSet = mockk<ResultSet>()
            val ordersPkResultSet = mockk<ResultSet>()
            val usersFkResultSet = mockk<ResultSet>()
            val ordersFkResultSet = mockk<ResultSet>()

            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema

            // Mock getTables() - "users" and "orders"
            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } returns tablesResultSet
            every { tablesResultSet.next() } returnsMany listOf(true, true, false)
            every { tablesResultSet.getString("TABLE_NAME") } returnsMany listOf("users", "orders")

            // Mock getColumns() for "users"
            every { metaData!!.getColumns(null, schema, "users", "%") } returns usersColumnsResultSet
            every { usersColumnsResultSet.next() } returnsMany listOf(true, false)
            every { usersColumnsResultSet.getString("COLUMN_NAME") } returns "id"
            every { usersColumnsResultSet.getString("TYPE_NAME") } returns "BIGINT"
            every { usersColumnsResultSet.getString("IS_NULLABLE") } returns "NO"

            // Mock getColumns() for "orders"
            every { metaData!!.getColumns(null, schema, "orders", "%") } returns ordersColumnsResultSet
            every { ordersColumnsResultSet.next() } returnsMany listOf(true, true, false)
            every { ordersColumnsResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "user_id")
            every { ordersColumnsResultSet.getString("TYPE_NAME") } returnsMany listOf("BIGINT", "BIGINT")
            every { ordersColumnsResultSet.getString("IS_NULLABLE") } returnsMany listOf("NO", "NO")

            // Mock getPrimaryKeys()
            every { metaData!!.getPrimaryKeys(null, schema, "users") } returns usersPkResultSet
            every { usersPkResultSet.next() } returnsMany listOf(true, false)
            every { usersPkResultSet.getString("COLUMN_NAME") } returns "id"

            every { metaData!!.getPrimaryKeys(null, schema, "orders") } returns ordersPkResultSet
            every { ordersPkResultSet.next() } returnsMany listOf(true, false)
            every { ordersPkResultSet.getString("COLUMN_NAME") } returns "id"

            // Mock getImportedKeys()
            every { metaData!!.getImportedKeys(null, schema, "users") } returns usersFkResultSet
            every { usersFkResultSet.next() } returns false

            every { metaData!!.getImportedKeys(null, schema, "orders") } returns ordersFkResultSet
            every { ordersFkResultSet.next() } returnsMany listOf(true, false)
            every { ordersFkResultSet.getString("FKCOLUMN_NAME") } returns "user_id"
            every { ordersFkResultSet.getString("PKTABLE_NAME") } returns "users"
            every { ordersFkResultSet.getString("PKCOLUMN_NAME") } returns "id"

            every { tablesResultSet.close() } just Runs
            every { usersColumnsResultSet.close() } just Runs
            every { ordersColumnsResultSet.close() } just Runs
            every { usersPkResultSet.close() } just Runs
            every { ordersPkResultSet.close() } just Runs
            every { usersFkResultSet.close() } just Runs
            every { ordersFkResultSet.close() } just Runs

            // when
            val result = introspector!!.introspect(dataSource!!)

            // then
            result.tables.size shouldBe 2

            val usersTable = result.tables.find { it.tableName == "users" }
            usersTable shouldNotBe null
            usersTable?.columns?.size shouldBe 1

            val ordersTable = result.tables.find { it.tableName == "orders" }
            ordersTable shouldNotBe null
            ordersTable?.columns?.size shouldBe 2

            val userIdColumn = ordersTable?.columns?.find { it.columnName == "user_id" }
            userIdColumn?.isForeignKey shouldBe true

            verify(exactly = 1) { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) }
        }

        test("should handle empty database (no tables)") {
            // given
            val schema = "empty_db"
            val tablesResultSet = mockk<ResultSet>()

            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema
            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } returns tablesResultSet
            every { tablesResultSet.next() } returns false
            every { tablesResultSet.close() } just Runs

            // when
            val result = introspector!!.introspect(dataSource!!)

            // then
            result.schemaName shouldBe schema
            result.tables.size shouldBe 0

            verify(exactly = 1) { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) }
            verify(exactly = 1) { connection!!.close() }
        }

        test("should extract nullable columns correctly") {
            // given
            val schema = "test_db"
            val tablesResultSet = mockk<ResultSet>()
            val columnsResultSet = mockk<ResultSet>()
            val primaryKeysResultSet = mockk<ResultSet>()
            val foreignKeysResultSet = mockk<ResultSet>()

            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema

            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } returns tablesResultSet
            every { tablesResultSet.next() } returnsMany listOf(true, false)
            every { tablesResultSet.getString("TABLE_NAME") } returns "users"

            every { metaData!!.getColumns(null, schema, "users", "%") } returns columnsResultSet
            every { columnsResultSet.next() } returnsMany listOf(true, true, false)
            every { columnsResultSet.getString("COLUMN_NAME") } returnsMany listOf("id", "nickname")
            every { columnsResultSet.getString("TYPE_NAME") } returnsMany listOf("BIGINT", "VARCHAR")
            every { columnsResultSet.getString("IS_NULLABLE") } returnsMany listOf("NO", "YES") // id NOT NULL, nickname NULL

            every { metaData!!.getPrimaryKeys(null, schema, "users") } returns primaryKeysResultSet
            every { primaryKeysResultSet.next() } returnsMany listOf(true, false)
            every { primaryKeysResultSet.getString("COLUMN_NAME") } returns "id"

            every { metaData!!.getImportedKeys(null, schema, "users") } returns foreignKeysResultSet
            every { foreignKeysResultSet.next() } returns false

            every { tablesResultSet.close() } just Runs
            every { columnsResultSet.close() } just Runs
            every { primaryKeysResultSet.close() } just Runs
            every { foreignKeysResultSet.close() } just Runs

            // when
            val result = introspector!!.introspect(dataSource!!)

            // then
            val idColumn = result.tables.first().columns.find { it.columnName == "id" }
            idColumn?.isNullable shouldBe false

            val nicknameColumn = result.tables.first().columns.find { it.columnName == "nickname" }
            nicknameColumn?.isNullable shouldBe true
        }
    }

    context("introspect - error handling") {

        test("should throw MetadataSyncException when connection fails") {
            // given
            every { dataSource!!.connection } throws RuntimeException("Connection refused")

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                introspector!!.introspect(dataSource!!)
            }

            exception.message shouldContain "JDBC introspection failed"
            exception.message shouldContain "Connection refused"

            verify(exactly = 1) { dataSource!!.connection }
        }

        test("should throw MetadataSyncException when getTables() fails") {
            // given
            val schema = "test_db"
            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema
            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } throws RuntimeException("Access denied")

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                introspector!!.introspect(dataSource!!)
            }

            exception.message shouldContain "JDBC introspection failed"
            exception.message shouldContain "Access denied"

            verify(exactly = 1) { connection.close() }
        }

        test("should close connection even when exception occurs") {
            // given
            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } throws RuntimeException("Catalog error")

            // when & then
            shouldThrow<MetadataSyncException> {
                introspector!!.introspect(dataSource!!)
            }

            verify(exactly = 1) { connection!!.close() }
        }
    }

    context("Real-world scenarios") {

        test("should filter out VIEWs and only extract TABLEs") {
            // given - Verify that only arrayOf("TABLE") is passed to getTables()
            val schema = "production_db"
            val tablesResultSet = mockk<ResultSet>()
            val columnsResultSet = mockk<ResultSet>()
            val primaryKeysResultSet = mockk<ResultSet>()
            val foreignKeysResultSet = mockk<ResultSet>()

            every { metaData!!.connection } returns connection!!
            every { connection!!.catalog } returns schema
            every { metaData!!.getTables(null, schema, "%", arrayOf("TABLE")) } returns tablesResultSet
            every { tablesResultSet.next() } returnsMany listOf(true, false)
            every { tablesResultSet.getString("TABLE_NAME") } returns "users"

            every { metaData!!.getColumns(null, schema, "users", "%") } returns columnsResultSet
            every { columnsResultSet.next() } returnsMany listOf(true, false)
            every { columnsResultSet.getString("COLUMN_NAME") } returns "id"
            every { columnsResultSet.getString("TYPE_NAME") } returns "BIGINT"
            every { columnsResultSet.getString("IS_NULLABLE") } returns "NO"

            every { metaData!!.getPrimaryKeys(null, schema, "users") } returns primaryKeysResultSet
            every { primaryKeysResultSet.next() } returns false

            every { metaData!!.getImportedKeys(null, schema, "users") } returns foreignKeysResultSet
            every { foreignKeysResultSet.next() } returns false

            every { tablesResultSet.close() } just Runs
            every { columnsResultSet.close() } just Runs
            every { primaryKeysResultSet.close() } just Runs
            every { foreignKeysResultSet.close() } just Runs

            // when
            val result = introspector!!.introspect(dataSource!!)

            // then
            result shouldNotBe null
            verify(exactly = 1) { metaData?.getTables(null, schema, "%", arrayOf("TABLE")) }
        }
    }
})
