package com.nbadal.ktlint.lib

data class RuleId(
    val value: String,
) {
    val ruleSetId: RuleSetId = RuleSetId(value.substringBefore(":"))
}
