package com.nbadal.ktlint.lib

import com.nbadal.ktlint.connector.KtlintVersion
import com.nbadal.ktlint.connector.KtlintVersionsConnector

class KtlintVersionsConnectorImpl : KtlintVersionsConnector {
    override val supportedKtlintVersions =
        KtlintRulesetVersion.entries.map { KtlintVersion(it.label(), it.alternativeRulesetVersion?.label()) }

    override fun findSupportedKtlintVersionByLabel(label: String?): KtlintVersion? =
        supportedKtlintVersions.firstOrNull { it.label == label }
}
