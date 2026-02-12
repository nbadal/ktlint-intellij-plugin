package com.nbadal.ktlint.connector

data class RuleId(
    val value: String,
) {
    val ruleSetId: RuleSetId = RuleSetId(value.substringBefore(":"))
}
