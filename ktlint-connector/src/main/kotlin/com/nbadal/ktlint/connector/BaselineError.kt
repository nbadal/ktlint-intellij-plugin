package com.nbadal.ktlint.connector

class BaselineError(
    val filePath: String,
    val line: Int,
    val col: Int,
    val ruleId: String,
)
