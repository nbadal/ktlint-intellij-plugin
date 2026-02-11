package com.nbadal.ktlint.lib

class BaselineError(
    val filePath: String,
    val line: Int,
    val col: Int,
    val ruleId: String,
)
