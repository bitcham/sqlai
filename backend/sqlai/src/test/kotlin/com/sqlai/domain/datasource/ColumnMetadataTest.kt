package com.sqlai.domain.datasource

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.context.SpringBootTest

/**
 * Unit tests for ColumnMetadata entity
 * Tests foreign key relation logic and validation
 */
@SpringBootTest
class ColumnMetadataTest : FunSpec({

    context("ColumnMetadata creation") {
        test("should create regular column without foreign key") {
            // given & when
            val column = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT",
                isNullable = false,
                isPrimaryKey = true
            )

            // then
            column.columnName shouldBe "id"
            column.dataType shouldBe "BIGINT"
            column.isNullable shouldBe false
            column.isPrimaryKey shouldBe true
            column.isForeignKey shouldBe false
            column.referencedTable shouldBe null
            column.referencedColumn shouldBe null
        }

        test("should create foreign key column") {
            // given & when
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isNullable = false,
                isForeignKey = true,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // then
            column.isForeignKey shouldBe true
            column.referencedTable shouldBe "users"
            column.referencedColumn shouldBe "id"
        }

        test("should use default values for optional fields") {
            // given & when
            val column = ColumnMetadata(
                columnName = "name",
                dataType = "VARCHAR"
            )

            // then
            column.isNullable shouldBe true
            column.isPrimaryKey shouldBe false
            column.isForeignKey shouldBe false
            column.referencedTable shouldBe null
            column.referencedColumn shouldBe null
        }

        test("should throw exception when columnName is blank") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "   ",
                    dataType = "VARCHAR"
                )
            }
            exception.message shouldContain "Column name must not be blank"
        }

        test("should throw exception when dataType is blank") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "name",
                    dataType = "   "
                )
            }
            exception.message shouldContain "Data type must not be blank"
        }

        test("should throw exception when FK has no referencedTable") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "user_id",
                    dataType = "BIGINT",
                    isForeignKey = true,
                    referencedTable = null,
                    referencedColumn = "id"
                )
            }
            exception.message shouldContain "Foreign key column must have referencedTable"
        }

        test("should throw exception when FK has blank referencedTable") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "user_id",
                    dataType = "BIGINT",
                    isForeignKey = true,
                    referencedTable = "   ",
                    referencedColumn = "id"
                )
            }
            exception.message shouldContain "Foreign key column must have referencedTable"
        }

        test("should throw exception when FK has no referencedColumn") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "user_id",
                    dataType = "BIGINT",
                    isForeignKey = true,
                    referencedTable = "users",
                    referencedColumn = null
                )
            }
            exception.message shouldContain "Foreign key column must have referencedColumn"
        }

        test("should throw exception when FK has blank referencedColumn") {
            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                ColumnMetadata(
                    columnName = "user_id",
                    dataType = "BIGINT",
                    isForeignKey = true,
                    referencedTable = "users",
                    referencedColumn = "   "
                )
            }
            exception.message shouldContain "Foreign key column must have referencedColumn"
        }

        test("should allow referencedTable without isForeignKey flag (legacy data)") {
            // given & when - Not recommended but technically allowed
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isForeignKey = false,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // then - Creates successfully but hasForeignKeyRelation() returns false
            column.isForeignKey shouldBe false
            column.referencedTable shouldBe "users"
        }
    }

    context("hasForeignKeyRelation() method") {
        test("should return true for valid foreign key") {
            // given
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isForeignKey = true,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // when
            val hasFk = column.hasForeignKeyRelation()

            // then
            hasFk shouldBe true
        }

        test("should return false when not a foreign key") {
            // given
            val column = ColumnMetadata(
                columnName = "name",
                dataType = "VARCHAR",
                isForeignKey = false
            )

            // when
            val hasFk = column.hasForeignKeyRelation()

            // then
            hasFk shouldBe false
        }

        test("should return false when isForeignKey is false despite having references") {
            // given - Edge case: has references but flag is false
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isForeignKey = false,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // when
            val hasFk = column.hasForeignKeyRelation()

            // then
            hasFk shouldBe false
        }
    }

    context("getForeignKeyReference() method") {
        test("should return formatted reference for valid FK") {
            // given
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isForeignKey = true,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // when
            val reference = column.getForeignKeyReference()

            // then
            reference shouldBe "users.id"
        }

        test("should return null for non-FK column") {
            // given
            val column = ColumnMetadata(
                columnName = "name",
                dataType = "VARCHAR"
            )

            // when
            val reference = column.getForeignKeyReference()

            // then
            reference shouldBe null
        }

        test("should return null when isForeignKey is false") {
            // given
            val column = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isForeignKey = false,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // when
            val reference = column.getForeignKeyReference()

            // then
            reference shouldBe null
        }

        test("should format various table and column names") {
            // Test different naming conventions
            val testCases = listOf(
                Triple("users", "id", "users.id"),
                Triple("order_items", "order_id", "order_items.order_id"),
                Triple("ProductCategories", "CategoryID", "ProductCategories.CategoryID"),
                Triple("t_user", "f_id", "t_user.f_id")
            )

            testCases.forEach { (table, col, expected) ->
                // given
                val column = ColumnMetadata(
                    columnName = "ref_col",
                    dataType = "BIGINT",
                    isForeignKey = true,
                    referencedTable = table,
                    referencedColumn = col
                )

                // when
                val reference = column.getForeignKeyReference()

                // then
                reference shouldBe expected
            }
        }
    }

    context("Entity identity") {
        test("should have null ID before persistence") {
            // given & when
            val column = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT"
            )

            // then
            column.id shouldBe null
        }

        test("should use default equals (reference equality)") {
            // given
            val column1 = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT"
            )
            val column2 = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT"
            )

            // then - Different instances
            (column1 === column2) shouldBe false
            (column1 == column2) shouldBe false
        }

        test("should be equal to itself") {
            // given
            val column = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT"
            )

            // then
            (column === column) shouldBe true
            (column == column) shouldBe true
        }
    }

    context("Business scenarios") {
        test("should model primary key column") {
            // given & when
            val pkColumn = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT",
                isNullable = false,
                isPrimaryKey = true
            )

            // then
            pkColumn.isPrimaryKey shouldBe true
            pkColumn.isNullable shouldBe false
            pkColumn.hasForeignKeyRelation() shouldBe false
        }

        test("should model regular data column") {
            // given & when
            val dataColumn = ColumnMetadata(
                columnName = "email",
                dataType = "VARCHAR",
                isNullable = false
            )

            // then
            dataColumn.isPrimaryKey shouldBe false
            dataColumn.isForeignKey shouldBe false
            dataColumn.isNullable shouldBe false
        }

        test("should model foreign key column") {
            // given & when
            val fkColumn = ColumnMetadata(
                columnName = "customer_id",
                dataType = "BIGINT",
                isNullable = false,
                isForeignKey = true,
                referencedTable = "customers",
                referencedColumn = "id"
            )

            // then
            fkColumn.isForeignKey shouldBe true
            fkColumn.hasForeignKeyRelation() shouldBe true
            fkColumn.getForeignKeyReference() shouldBe "customers.id"
        }

        test("should model nullable column") {
            // given & when
            val nullableColumn = ColumnMetadata(
                columnName = "middle_name",
                dataType = "VARCHAR",
                isNullable = true
            )

            // then
            nullableColumn.isNullable shouldBe true
        }

        test("should support various data types") {
            val dataTypes = listOf(
                "BIGINT", "INTEGER", "SMALLINT",
                "VARCHAR", "TEXT", "CHAR",
                "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE",
                "DATE", "TIMESTAMP", "TIME",
                "BOOLEAN", "BLOB", "JSON"
            )

            dataTypes.forEach { type ->
                // given & when
                val column = ColumnMetadata(
                    columnName = "test_col",
                    dataType = type
                )

                // then
                column.dataType shouldBe type
            }
        }

        test("should model composite foreign key scenario") {
            // Composite FK: order_items(order_id, product_id) references orders(id) and products(id)

            // First FK
            val fk1 = ColumnMetadata(
                columnName = "order_id",
                dataType = "BIGINT",
                isForeignKey = true,
                referencedTable = "orders",
                referencedColumn = "id"
            )

            // Second FK
            val fk2 = ColumnMetadata(
                columnName = "product_id",
                dataType = "BIGINT",
                isForeignKey = true,
                referencedTable = "products",
                referencedColumn = "id"
            )

            // Verify both
            fk1.getForeignKeyReference() shouldBe "orders.id"
            fk2.getForeignKeyReference() shouldBe "products.id"
        }

        test("should handle self-referential foreign key") {
            // given - employee.manager_id references employee.id
            val selfRefColumn = ColumnMetadata(
                columnName = "manager_id",
                dataType = "BIGINT",
                isNullable = true,
                isForeignKey = true,
                referencedTable = "employees",
                referencedColumn = "id"
            )

            // then
            selfRefColumn.hasForeignKeyRelation() shouldBe true
            selfRefColumn.getForeignKeyReference() shouldBe "employees.id"
        }

        test("should model typical table structure") {
            // given - users table columns
            val idColumn = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT",
                isNullable = false,
                isPrimaryKey = true
            )

            val emailColumn = ColumnMetadata(
                columnName = "email",
                dataType = "VARCHAR",
                isNullable = false
            )

            val createdAtColumn = ColumnMetadata(
                columnName = "created_at",
                dataType = "TIMESTAMP",
                isNullable = false
            )

            // given - orders table with FK to users
            val orderIdColumn = ColumnMetadata(
                columnName = "id",
                dataType = "BIGINT",
                isNullable = false,
                isPrimaryKey = true
            )

            val userIdColumn = ColumnMetadata(
                columnName = "user_id",
                dataType = "BIGINT",
                isNullable = false,
                isForeignKey = true,
                referencedTable = "users",
                referencedColumn = "id"
            )

            // Verify structure
            idColumn.isPrimaryKey shouldBe true
            emailColumn.isNullable shouldBe false
            orderIdColumn.isPrimaryKey shouldBe true
            userIdColumn.getForeignKeyReference() shouldBe "users.id"
        }
    }

})