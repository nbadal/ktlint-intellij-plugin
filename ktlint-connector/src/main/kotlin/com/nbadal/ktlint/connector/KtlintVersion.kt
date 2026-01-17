package com.nbadal.ktlint.connector

@JvmInline
value class KtlintVersion(
    val value: String,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
