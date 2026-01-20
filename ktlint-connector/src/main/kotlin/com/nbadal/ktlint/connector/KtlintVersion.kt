package com.nbadal.ktlint.connector

import java.io.Serializable

data class KtlintVersion(
    val label: String,
    val alternativeKtlintVersionLabel: String? = null,
) : Serializable {
    companion object {
        val DEFAULT = KtlintVersion("DEFAULT")
    }
}
