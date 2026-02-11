package com.nbadal.ktlint.lib

data class KtlintVersion(
    var label: String = "",
    var alternativeKtlintVersionLabel: String? = null,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
