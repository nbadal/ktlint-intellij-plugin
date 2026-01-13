package com.nbadal.ktlint.connector

data class LintError(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
    val detail: String,
    val canBeAutoCorrected: Boolean,
)
