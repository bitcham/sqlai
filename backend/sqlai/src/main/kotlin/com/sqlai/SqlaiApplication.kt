package com.sqlai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SqlaiApplication

fun main(args: Array<String>) {
    runApplication<SqlaiApplication>(*args)
}
