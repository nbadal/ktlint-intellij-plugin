package com.nbadal.ktlint.connector

data class KtlintVersion(
    val label: String,
    val alternativeKtlintVersionLabel: String? = null,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
