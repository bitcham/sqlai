package com.sqlai.service.ai

import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SchemaFormatterTest : FunSpec({

    val formatter = SchemaFormatter()

    test("should format single table with simple columns") {
        // given
        val metadata = DatabaseMetadata("test_schema")
        val table = TableMetadata("users")
        table.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        table.columns.add(ColumnMetadata("name", "VARCHAR(255)", true, false, false))
        metadata.tables.add(table)

        // when
        val result = formatter.format(metadata)

        // then
        result shouldContain "Table: users"
        result shouldContain "id: BIGINT (Primary Key)"
        result shouldContain "name: VARCHAR(255)"
    }

    test("should format multiple tables") {
        // given
        val metadata = DatabaseMetadata("test_schema")

        val customersTable = TableMetadata("customers")
        customersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        customersTable.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))
        metadata.tables.add(customersTable)

        val ordersTable = TableMetadata("orders")
        ordersTable.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        ordersTable.columns.add(ColumnMetadata("customer_id", "BIGINT", false, false, true, "customers", "id"))
        metadata.tables.add(ordersTable)

        // when
        val result = formatter.format(metadata)

        // then
        result shouldContain "Table: customers"
        result shouldContain "Table: orders"
        result shouldContain "customer_id: BIGINT (Foreign Key -> customers.id)"
    }

    test("should format foreign key relationships correctly") {
        // given
        val metadata = DatabaseMetadata("test_schema")
        val table = TableMetadata("orders")
        table.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        table.columns.add(ColumnMetadata("customer_id", "BIGINT", false, false, true, "customers", "id"))
        table.columns.add(ColumnMetadata("product_id", "BIGINT", false, false, true, "products", "id"))
        metadata.tables.add(table)

        // when
        val result = formatter.format(metadata)

        // then
        result shouldContain "customer_id: BIGINT (Foreign Key -> customers.id)"
        result shouldContain "product_id: BIGINT (Foreign Key -> products.id)"
    }

    test("should handle empty database metadata") {
        // given
        val metadata = DatabaseMetadata("empty_schema")

        // when
        val result = formatter.format(metadata)

        // then
        result shouldBe ""
    }

    test("should format complex schema with multiple tables and relationships") {
        // given
        val metadata = DatabaseMetadata("ecommerce")

        // customers table
        val customers = TableMetadata("customers")
        customers.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        customers.columns.add(ColumnMetadata("name", "VARCHAR(255)", false, false, false))
        customers.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))
        metadata.tables.add(customers)

        // orders table
        val orders = TableMetadata("orders")
        orders.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        orders.columns.add(ColumnMetadata("customer_id", "BIGINT", false, false, true, "customers", "id"))
        orders.columns.add(ColumnMetadata("order_date", "DATE", false, false, false))
        metadata.tables.add(orders)

        // order_items table
        val orderItems = TableMetadata("order_items")
        orderItems.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        orderItems.columns.add(ColumnMetadata("order_id", "BIGINT", false, false, true, "orders", "id"))
        orderItems.columns.add(ColumnMetadata("product_id", "BIGINT", false, false, true, "products", "id"))
        orderItems.columns.add(ColumnMetadata("quantity", "INT", false, false, false))
        metadata.tables.add(orderItems)

        // products table
        val products = TableMetadata("products")
        products.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
        products.columns.add(ColumnMetadata("name", "VARCHAR(255)", false, false, false))
        products.columns.add(ColumnMetadata("category", "VARCHAR(100)", true, false, false))
        metadata.tables.add(products)

        // when
        val result = formatter.format(metadata)

        // then
        result shouldContain "Table: customers"
        result shouldContain "Table: orders"
        result shouldContain "Table: order_items"
        result shouldContain "Table: products"
        result shouldContain "order_id: BIGINT (Foreign Key -> orders.id)"
        result shouldContain "product_id: BIGINT (Foreign Key -> products.id)"
        result shouldContain "category: VARCHAR(100)"
    }
})
