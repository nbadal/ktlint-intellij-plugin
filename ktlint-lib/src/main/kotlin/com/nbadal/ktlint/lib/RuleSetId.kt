package com.nbadal.ktlint.lib

data class RuleSetId(
    val value: String,
) {
    companion object {
        val STANDARD = RuleSetId("standard")
    }
}
