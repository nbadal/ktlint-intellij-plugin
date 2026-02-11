package com.nbadal.ktlint.lib

data class LintError(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
    val detail: String,
    val canBeAutoCorrected: Boolean,
)
