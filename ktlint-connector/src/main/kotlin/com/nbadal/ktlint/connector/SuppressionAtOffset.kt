package com.nbadal.ktlint.connector

data class SuppressionAtOffset(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
)
