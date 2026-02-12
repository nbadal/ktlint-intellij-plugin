package com.nbadal.ktlint.connector

interface KtlintVersionsConnector {
    val supportedKtlintVersions: List<KtlintVersion>

    fun findSupportedKtlintVersionByLabel(label: String?): KtlintVersion?
}

data class KtlintVersion(
    var label: String = "",
    var alternativeKtlintVersionLabel: String? = null,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
