package com.nbadal.ktlint.connector

data class KtlintVersion(
    var label: String = "",
    var alternativeKtlintVersionLabel: String? = null,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
