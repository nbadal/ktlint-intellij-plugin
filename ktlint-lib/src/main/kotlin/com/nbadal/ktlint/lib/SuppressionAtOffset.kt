package com.nbadal.ktlint.lib

data class SuppressionAtOffset(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
)
