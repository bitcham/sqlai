package com.sqlai.service.ai

import com.sqlai.config.ExecutionPolicyProperties
import com.sqlai.config.PromptBuilderProperties
import com.sqlai.domain.datasource.DatabaseMetadata
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

/**
 * Service for building AI prompts from templates
 * Assembles: prefix + user_input + infix + schema_metadata + suffix
 */
@Service
class PromptBuilder(
    private val schemaFormatter: SchemaFormatter,
    private val executionPolicyProperties: ExecutionPolicyProperties,
    private val promptBuilderProperties: PromptBuilderProperties
) {

    private val prefixTemplate: String by lazy {
        loadTemplate("sql_prompt_prefix.txt")
    }

    private val infixTemplate: String by lazy {
        loadTemplate("sql_prompt_infix.txt")
    }

    private val suffixTemplate: String by lazy {
        loadTemplate("sql_prompt_suffix.txt")
    }

    /**
     * Build complete prompt for AI SQL generation
     *
     * @param userInput Natural language question from user
     * @param metadata Database schema metadata
     * @return Complete prompt ready to send to AI provider
     */
    fun buildPrompt(userInput: String, metadata: DatabaseMetadata): String {
        val schemaText = schemaFormatter.format(metadata)

        // Build prompt: prefix + user_input + infix + schema + suffix
        val prompt = StringBuilder()

        // Add prefix with placeholders replaced
        prompt.append(prefixTemplate)
        prompt.append("\n\n")

        // Add user input section
        prompt.append(userInput)
        prompt.append("\n\n")

        // Add infix
        prompt.append(infixTemplate)
        prompt.append("\n")

        // Add schema metadata
        prompt.append(schemaText)
        prompt.append("\n\n")

        // Add suffix with placeholders replaced
        prompt.append(replacePlaceholders(suffixTemplate, userInput, schemaText))

        return prompt.toString()
    }

    private fun replacePlaceholders(template: String, userInput: String, schemaText: String): String {
        return template
            .replace("{database_type}", promptBuilderProperties.databaseType)
            .replace("{max_limit}", executionPolicyProperties.maxRowLimit.toString())
    }

    private fun extractTableNames(schemaText: String): String {
        val tableNames = schemaText
            .lines()
            .filter { it.startsWith("Table: ") }.joinToString(", ") { it.removePrefix("Table: ").trim() }
        return tableNames.ifEmpty { "No tables available" }
    }

    private fun loadTemplate(fileName: String): String {
        return try {
            val resource = ClassPathResource(fileName)
            resource.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load prompt template: $fileName", e)
        }
    }
}
