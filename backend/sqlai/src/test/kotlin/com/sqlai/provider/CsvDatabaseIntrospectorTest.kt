package com.sqlai.provider

import com.sqlai.exception.MetadataSyncException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import java.io.File
import javax.sql.DataSource

class CsvDatabaseIntrospectorTest : FunSpec({

    lateinit var dataSource: DataSource

    beforeEach {
        dataSource = mockk()
    }

    context("introspect - successful CSV file parsing") {

        test("should extract metadata from single CSV file") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "users.csv")
            csvFile.writeText("""
                id,email,age
                1,user1@example.com,25
                2,user2@example.com,30
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.schemaName shouldBe "csv_datasource"
            result.tables.size shouldBe 1

            val usersTable = result.tables.first()
            usersTable.tableName shouldBe "users"
            usersTable.columns.size shouldBe 3

            val idColumn = usersTable.columns.find { it.columnName == "id" }
            idColumn?.dataType shouldBe "INTEGER"
            idColumn?.isNullable shouldBe true

            val emailColumn = usersTable.columns.find { it.columnName == "email" }
            emailColumn?.dataType shouldBe "TEXT"

            val ageColumn = usersTable.columns.find { it.columnName == "age" }
            ageColumn?.dataType shouldBe "INTEGER"

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should extract metadata from multiple CSV files") {
            // given
            val tempDir = createTempDir("csv_test_")

            val usersFile = File(tempDir, "users.csv")
            usersFile.writeText("""
                id,name
                1,Alice
                2,Bob
            """.trimIndent())

            val ordersFile = File(tempDir, "orders.csv")
            ordersFile.writeText("""
                order_id,amount
                100,99.99
                101,149.50
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.tables.size shouldBe 2

            val usersTable = result.tables.find { it.tableName == "users" }
            usersTable?.columns?.size shouldBe 2

            val ordersTable = result.tables.find { it.tableName == "orders" }
            ordersTable?.columns?.size shouldBe 2

            val amountColumn = ordersTable?.columns?.find { it.columnName == "amount" }
            amountColumn?.dataType shouldBe "DECIMAL"

            // cleanup
            usersFile.delete()
            ordersFile.delete()
            tempDir.delete()
        }

        test("should infer INTEGER data type for integer values") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "test.csv")
            csvFile.writeText("""
                id,count,quantity
                1,100,50
                2,200,75
                3,300,25
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()
            table.columns.forEach { column ->
                column.dataType shouldBe "INTEGER"
            }

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should infer DECIMAL data type for decimal values") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "prices.csv")
            csvFile.writeText("""
                product,price
                Apple,1.99
                Banana,0.59
                Orange,2.49
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()

            val productColumn = table.columns.find { it.columnName == "product" }
            productColumn?.dataType shouldBe "TEXT"

            val priceColumn = table.columns.find { it.columnName == "price" }
            priceColumn?.dataType shouldBe "DECIMAL"

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should infer DATE data type for date values (yyyy-MM-dd)") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "events.csv")
            csvFile.writeText("""
                event_name,event_date
                Launch,2024-01-15
                Conference,2024-03-20
                Meetup,2024-06-10
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()

            val eventNameColumn = table.columns.find { it.columnName == "event_name" }
            eventNameColumn?.dataType shouldBe "TEXT"

            val eventDateColumn = table.columns.find { it.columnName == "event_date" }
            eventDateColumn?.dataType shouldBe "DATE"

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should infer TEXT data type for mixed values") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "mixed.csv")
            csvFile.writeText("""
                field
                hello
                123
                world
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()
            table.columns.first().dataType shouldBe "TEXT"

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should handle empty CSV file") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "empty.csv")
            csvFile.writeText("")

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.tables.size shouldBe 1
            result.tables.first().tableName shouldBe "empty"
            result.tables.first().columns.size shouldBe 0

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should handle CSV with only headers (no data rows)") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "headers_only.csv")
            csvFile.writeText("id,name,email")

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()
            table.tableName shouldBe "headers_only"
            table.columns.size shouldBe 3

            // Without data rows, all columns default to TEXT
            table.columns.forEach { column ->
                column.dataType shouldBe "TEXT"
            }

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should handle empty directory (no CSV files)") {
            // given
            val tempDir = createTempDir("csv_test_empty_")
            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.schemaName shouldBe "csv_datasource"
            result.tables.size shouldBe 0

            // cleanup
            tempDir.delete()
        }

        test("should ignore non-CSV files in directory") {
            // given
            val tempDir = createTempDir("csv_test_")

            val csvFile = File(tempDir, "data.csv")
            csvFile.writeText("id,value\n1,test")

            val txtFile = File(tempDir, "readme.txt")
            txtFile.writeText("This is a text file")

            val jsonFile = File(tempDir, "config.json")
            jsonFile.writeText("{\"key\": \"value\"}")

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.tables.size shouldBe 1
            result.tables.first().tableName shouldBe "data"

            // cleanup
            csvFile.delete()
            txtFile.delete()
            jsonFile.delete()
            tempDir.delete()
        }
    }

    context("introspect - error handling") {

        test("should throw MetadataSyncException when directory does not exist") {
            // given
            val introspector = CsvDatabaseIntrospector(csvDirectory = "/nonexistent/path")

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                introspector.introspect(dataSource)
            }

            exception.message shouldContain "CSV directory does not exist"
        }

        test("should throw MetadataSyncException when path is not a directory") {
            // given
            val tempFile = createTempFile("not_a_dir")
            val introspector = CsvDatabaseIntrospector(csvDirectory = tempFile.absolutePath)

            // when & then
            val exception = shouldThrow<MetadataSyncException> {
                introspector.introspect(dataSource)
            }

            exception.message shouldContain "CSV path is not a directory"

            // cleanup
            tempFile.delete()
        }

        test("should handle empty CSV files gracefully") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "empty.csv")
            csvFile.writeText("") // Completely empty file

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            result.tables shouldHaveSize 1
            result.tables.first().tableName shouldBe "empty"
            result.tables.first().columns.shouldBeEmpty()

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }
    }

    context("Real-world scenarios") {

        test("should extract sales data from CSV") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "sales_2024.csv")
            csvFile.writeText("""
                date,product,revenue,quantity
                2024-01-01,Widget A,1500.50,10
                2024-01-02,Widget B,2300.75,15
                2024-01-03,Widget C,999.99,8
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()
            table.tableName shouldBe "sales_2024"
            table.columns.size shouldBe 4

            val dateColumn = table.columns.find { it.columnName == "date" }
            dateColumn?.dataType shouldBe "DATE"

            val productColumn = table.columns.find { it.columnName == "product" }
            productColumn?.dataType shouldBe "TEXT"

            val revenueColumn = table.columns.find { it.columnName == "revenue" }
            revenueColumn?.dataType shouldBe "DECIMAL"

            val quantityColumn = table.columns.find { it.columnName == "quantity" }
            quantityColumn?.dataType shouldBe "INTEGER"

            // All CSV columns are nullable
            table.columns.forEach { column ->
                column.isNullable shouldBe true
                column.isPrimaryKey shouldBe false
                column.isForeignKey shouldBe false
            }

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }

        test("should handle CSV with whitespace in values") {
            // given
            val tempDir = createTempDir("csv_test_")
            val csvFile = File(tempDir, "data.csv")
            csvFile.writeText("""
                name, age , city
                Alice , 25 , New York
                Bob , 30 , San Francisco
            """.trimIndent())

            val introspector = CsvDatabaseIntrospector(csvDirectory = tempDir.absolutePath)

            // when
            val result = introspector.introspect(dataSource)

            // then
            val table = result.tables.first()
            table.columns.size shouldBe 3

            val nameColumn = table.columns.find { it.columnName == "name" }
            nameColumn shouldBe table.columns[0]

            val ageColumn = table.columns.find { it.columnName == "age" }
            ageColumn?.dataType shouldBe "INTEGER"

            val cityColumn = table.columns.find { it.columnName == "city" }
            cityColumn?.dataType shouldBe "TEXT"

            // cleanup
            csvFile.delete()
            tempDir.delete()
        }
    }
})
