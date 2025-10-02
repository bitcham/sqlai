package com.sqlai.service.query

import com.sqlai.config.ExecutionPolicyProperties
import com.sqlai.dto.ExecutionStatus
import com.sqlai.exception.QueryExecutionException
import com.sqlai.exception.UnsafeSqlException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLTimeoutException

class QueryExecutionServiceTest : FunSpec({

    lateinit var jdbcTemplate: JdbcTemplate
    lateinit var executionPolicyProperties: ExecutionPolicyProperties
    lateinit var service: QueryExecutionService

    beforeEach {
        jdbcTemplate = mockk()
        executionPolicyProperties = ExecutionPolicyProperties(
            maxExecutionTimeMs = 30000,
            maxRowLimit = 1000,
            allowedOperations = listOf("SELECT"),
            queryTimeoutSeconds = 30
        )
        service = QueryExecutionService(jdbcTemplate, executionPolicyProperties)
    }

    afterEach {
        clearAllMocks()
    }

    context("execute - successful query execution") {

        test("should execute SELECT query and return results") {
            // given
            val sql = "SELECT id, name FROM users"
            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 2
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { metaData.getColumnName(2) } returns "name"
            every { metaData.getColumnTypeName(2) } returns "VARCHAR"

            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, true, false)
            every { resultSet.getObject("id") } returnsMany listOf(1L, 2L)
            every { resultSet.getObject("name") } returnsMany listOf("Alice", "Bob")

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 2
            result.data.size shouldBe 2
            result.columns.size shouldBe 2
            result.columns[0].name shouldBe "id"
            result.columns[0].type shouldBe "BIGINT"
            result.columns[1].name shouldBe "name"
            result.columns[1].type shouldBe "VARCHAR"
            result.errorMessage shouldBe null

            verify(exactly = 1) { jdbcTemplate.query(sql, any<RowCallbackHandler>()) }
        }

        test("should execute query and return empty results") {
            // given
            val sql = "SELECT id FROM users WHERE 1=0"
            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 1
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returns false

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 0
            result.data.size shouldBe 0
            result.columns.size shouldBe 1
            result.executionTimeMs shouldNotBe null
        }

        test("should track execution time") {
            // given
            val sql = "SELECT * FROM users"
            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 1
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returns false

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                Thread.sleep(50) // Simulate query execution
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.executionTimeMs shouldNotBe null
            result.executionTimeMs shouldNotBe 0
        }
    }

    context("execute - SQL safety validation") {

        test("should throw UnsafeSqlException for INSERT statement") {
            // given
            val sql = "INSERT INTO users (name) VALUES ('test')"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
            verify(exactly = 0) { jdbcTemplate.query(any<String>(), any<RowCallbackHandler>()) }
        }

        test("should throw UnsafeSqlException for UPDATE statement") {
            // given
            val sql = "UPDATE users SET name = 'test' WHERE id = 1"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
        }

        test("should throw UnsafeSqlException for DELETE statement") {
            // given
            val sql = "DELETE FROM users WHERE id = 1"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
        }

        test("should throw UnsafeSqlException for DROP statement") {
            // given
            val sql = "DROP TABLE users"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
        }

        test("should throw UnsafeSqlException for CREATE statement") {
            // given
            val sql = "CREATE TABLE test (id INT)"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
        }

        test("should throw UnsafeSqlException for TRUNCATE statement") {
            // given
            val sql = "TRUNCATE TABLE users"

            // when/then
            val exception = shouldThrow<UnsafeSqlException> {
                service.execute(sql)
            }

            exception.message shouldBe "Only SELECT queries are allowed"
        }

        test("should allow SELECT with safe subquery") {
            // given
            val sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)"
            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 1
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returns false

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
        }
    }

    context("execute - row limit enforcement") {

        test("should enforce max row limit") {
            // given
            val sql = "SELECT id FROM users"
            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 1
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { resultSet.metaData } returns metaData

            // Simulate 1500 rows available (more than maxRowLimit = 1000)
            val nextResults = MutableList(1500) { true } + false
            every { resultSet.next() } returnsMany nextResults
            every { resultSet.getObject("id") } returnsMany (1L..1500L).toList()

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 1000 // Should be limited to maxRowLimit
            result.data.size shouldBe 1000
        }
    }

    context("execute - timeout handling") {

        test("should return TIMEOUT status when SQLTimeoutException occurs") {
            // given
            val sql = "SELECT * FROM large_table"

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } throws SQLTimeoutException()

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.TIMEOUT
            result.rowCount shouldBe 0
            result.data.size shouldBe 0
            result.errorMessage shouldNotBe null
            result.errorMessage shouldBe "Query execution exceeded 30000ms timeout"
        }
    }

    context("execute - error handling") {

        test("should throw QueryExecutionException for generic SQL errors") {
            // given
            val sql = "SELECT * FROM non_existent_table"
            val sqlException = RuntimeException("Table does not exist")

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } throws sqlException

            // when/then
            val exception = shouldThrow<QueryExecutionException> {
                service.execute(sql)
            }

            exception.message shouldBe "Failed to execute query: Table does not exist"
        }

        test("should throw IllegalArgumentException for blank SQL") {
            // given
            val sql = "   "

            // when/then
            shouldThrow<IllegalArgumentException> {
                service.execute(sql)
            }
        }

        test("should throw IllegalArgumentException for empty SQL") {
            // given
            val sql = ""

            // when/then
            shouldThrow<IllegalArgumentException> {
                service.execute(sql)
            }
        }
    }

    context("validateSqlSafety - safety checks") {

        test("should validate SELECT query as safe") {
            // given
            val sql = "SELECT * FROM users"

            // when
            val result = service.validateSqlSafety(sql)

            // then
            result shouldBe true
        }

        test("should validate SELECT with JOIN as safe") {
            // given
            val sql = "SELECT u.id, o.total FROM users u JOIN orders o ON u.id = o.user_id"

            // when
            val result = service.validateSqlSafety(sql)

            // then
            result shouldBe true
        }

        test("should validate SELECT with WHERE clause as safe") {
            // given
            val sql = "SELECT * FROM users WHERE age > 18 AND country = 'US'"

            // when
            val result = service.validateSqlSafety(sql)

            // then
            result shouldBe true
        }

        test("should reject non-SELECT query") {
            // given
            val sql = "UPDATE users SET active = true"

            // when/then
            shouldThrow<UnsafeSqlException> {
                service.validateSqlSafety(sql)
            }
        }

        test("should handle case-insensitive validation") {
            // given
            val sql = "select * from users"

            // when
            val result = service.validateSqlSafety(sql)

            // then
            result shouldBe true
        }

        test("should handle SQL with leading whitespace") {
            // given
            val sql = "   SELECT * FROM users"

            // when
            val result = service.validateSqlSafety(sql)

            // then
            result shouldBe true
        }
    }

    context("Real-world scenarios") {

        test("should execute complex JOIN query") {
            // given
            val sql = """
                SELECT u.name, COUNT(o.id) as order_count
                FROM users u
                LEFT JOIN orders o ON u.id = o.user_id
                GROUP BY u.name
                ORDER BY order_count DESC
            """.trimIndent()

            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 2
            every { metaData.getColumnName(1) } returns "name"
            every { metaData.getColumnTypeName(1) } returns "VARCHAR"
            every { metaData.getColumnName(2) } returns "order_count"
            every { metaData.getColumnTypeName(2) } returns "BIGINT"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getObject("name") } returns "Alice"
            every { resultSet.getObject("order_count") } returns 5L

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 1
            result.columns.size shouldBe 2
        }

        test("should execute aggregation query") {
            // given
            val sql = "SELECT SUM(total_amount) as total_revenue, AVG(total_amount) as avg_revenue FROM orders"

            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 2
            every { metaData.getColumnName(1) } returns "total_revenue"
            every { metaData.getColumnTypeName(1) } returns "DECIMAL"
            every { metaData.getColumnName(2) } returns "avg_revenue"
            every { metaData.getColumnTypeName(2) } returns "DECIMAL"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getObject("total_revenue") } returns 10000.50
            every { resultSet.getObject("avg_revenue") } returns 250.25

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 1
            result.data[0]["total_revenue"] shouldBe 10000.50
            result.data[0]["avg_revenue"] shouldBe 250.25
        }

        test("should handle query with NULL values") {
            // given
            val sql = "SELECT id, middle_name FROM users"

            val resultSet = mockk<ResultSet>()
            val metaData = mockk<ResultSetMetaData>()

            every { metaData.columnCount } returns 2
            every { metaData.getColumnName(1) } returns "id"
            every { metaData.getColumnTypeName(1) } returns "BIGINT"
            every { metaData.getColumnName(2) } returns "middle_name"
            every { metaData.getColumnTypeName(2) } returns "VARCHAR"
            every { resultSet.metaData } returns metaData
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getObject("id") } returns 1L
            every { resultSet.getObject("middle_name") } returns null

            every { jdbcTemplate.query(sql, any<RowCallbackHandler>()) } answers {
                val handler = secondArg<RowCallbackHandler>()
                handler.processRow(resultSet)
            }

            // when
            val result = service.execute(sql)

            // then
            result.status shouldBe ExecutionStatus.SUCCESS
            result.rowCount shouldBe 1
            result.data[0]["id"] shouldBe 1L
            result.data[0]["middle_name"] shouldBe null
        }
    }
})
