package com.sqlai.service.ai

import com.sqlai.config.ExecutionPolicyProperties
import com.sqlai.config.PromptBuilderProperties
import com.sqlai.domain.datasource.ColumnMetadata
import com.sqlai.domain.datasource.DatabaseMetadata
import com.sqlai.domain.datasource.TableMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotContain

class PromptBuilderTest : FunSpec({

    val schemaFormatter = SchemaFormatter()
    val executionPolicyProperties = ExecutionPolicyProperties(
        maxExecutionTimeMs = 30000,
        maxRowLimit = 1000,
        allowedOperations = listOf("SELECT"),
        queryTimeoutSeconds = 30
    )
    val promptBuilderProperties = PromptBuilderProperties(
        databaseType = "H2"
    )
    val promptBuilder = PromptBuilder(schemaFormatter, executionPolicyProperties, promptBuilderProperties)

    test("should build prompt with all sections") {
        // given
        val userInput = "Show all customers who bought electronics in Sep 2025"
        val metadata = createSampleMetadata()

        // when
        val prompt = promptBuilder.buildPrompt(userInput, metadata)

        // then
        prompt.shouldNotBeEmpty()
        prompt shouldContain "## User Request"
        prompt shouldContain userInput
        prompt shouldContain "## Data Metadata"
        prompt shouldContain "Table: customers"
        prompt shouldContain "Table: orders"
    }

    test("should include schema information in prompt") {
        // given
        val userInput = "Count all orders"
        val metadata = createSampleMetadata()

        // when
        val prompt = promptBuilder.buildPrompt(userInput, metadata)

        // then
        prompt shouldContain "customers"
        prompt shouldContain "orders"
        prompt shouldContain "id: BIGINT (Primary Key)"
        prompt shouldContain "customer_id: BIGINT (Foreign Key -> customers.id)"
    }

    test("should replace placeholders in templates") {
        // given
        val userInput = "Get customer details"
        val metadata = createSampleMetadata()

        // when
        val prompt = promptBuilder.buildPrompt(userInput, metadata)

        // then
        prompt shouldNotContain "{user_request}"
        prompt shouldNotContain "{schema_info}"
        prompt shouldNotContain "{table_list}"
        prompt shouldNotContain "{database_type}"
        prompt shouldNotContain "{max_limit}"
        // Verify placeholders are replaced with actual values
        prompt shouldContain "H2"
        prompt shouldContain "1000"
    }

    test("should handle empty schema") {
        // given
        val userInput = "Show data"
        val metadata = DatabaseMetadata("empty_schema")

        // when
        val prompt = promptBuilder.buildPrompt(userInput, metadata)

        // then
        prompt shouldNotBe null
        prompt shouldContain userInput
        prompt shouldContain "Database Type: H2"
        prompt shouldContain "Max Result Limit: 1000"
    }

    test("should include table names in schema") {
        // given
        val userInput = "Query tables"
        val metadata = createSampleMetadata()

        // when
        val prompt = promptBuilder.buildPrompt(userInput, metadata)

        // then
        prompt shouldContain "Table: customers"
        prompt shouldContain "Table: orders"
    }
})

private fun createSampleMetadata(): DatabaseMetadata {
    val metadata = DatabaseMetadata("test_db")

    val customers = TableMetadata("customers")
    customers.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
    customers.columns.add(ColumnMetadata("name", "VARCHAR(255)", false, false, false))
    customers.columns.add(ColumnMetadata("email", "VARCHAR(255)", false, false, false))
    metadata.tables.add(customers)

    val orders = TableMetadata("orders")
    orders.columns.add(ColumnMetadata("id", "BIGINT", false, true, false))
    orders.columns.add(ColumnMetadata("customer_id", "BIGINT", false, false, true, "customers", "id"))
    orders.columns.add(ColumnMetadata("order_date", "DATE", false, false, false))
    metadata.tables.add(orders)

    return metadata
}
