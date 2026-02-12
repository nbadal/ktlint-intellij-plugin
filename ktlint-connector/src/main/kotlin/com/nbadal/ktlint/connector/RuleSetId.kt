package com.nbadal.ktlint.connector

data class RuleSetId(
    val value: String,
) {
    companion object {
        val STANDARD = RuleSetId("standard")
    }
}
