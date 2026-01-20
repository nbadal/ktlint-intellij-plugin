package com.nbadal.ktlint.connector

import java.io.Serializable

data class KtlintVersion(
    var label: String = "",
    var alternativeKtlintVersionLabel: String? = null,
) {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
