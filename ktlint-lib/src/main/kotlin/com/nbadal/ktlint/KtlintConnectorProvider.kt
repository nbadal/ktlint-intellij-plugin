package com.nbadal.ktlint

import com.nbadal.ktlint.connector.KtlintConnector
import com.nbadal.ktlint.connector.KtlintConnectorException

// TODO: Does this class add any value now the KtlintConnector also does the loading?
class KtlintConnectorProvider {
    /**
     * The set of ruleset providers that are loaded into the KtLintRuleEngine
     */
    private lateinit var ruleSetProviders: RuleSetProviders

    private lateinit var _ktlintConnector: KtlintConnector

    val ktlintConnector: KtlintConnector
        get() = _ktlintConnector

    fun configure(
        ktlintRulesetVersion: KtlintRulesetVersion,
        externalJarPaths: List<String>,
    ) {
        if (!::ruleSetProviders.isInitialized ||
            ruleSetProviders.ktlintRulesetVersion != ktlintRulesetVersion ||
            ruleSetProviders.externalJarPaths != externalJarPaths
        ) {
            ktlintLibLogger.info("Configure KtlintRuleEngineWrapper $ktlintRulesetVersion, $externalJarPaths")
            ruleSetProviders = RuleSetProviders(ktlintRulesetVersion, externalJarPaths)
            _ktlintConnector = KtlintConnector.getInstance()!!
            try {
                ktlintConnector.loadRulesets(ktlintRulesetVersion.label(), externalJarPaths)
            } catch (ktlintConnectorException: KtlintConnectorException) {
                // TODO: Send message to Notifier???
                ktlintLibLogger.error(ktlintConnectorException)
            }
        }
    }

    fun errorLoadingExternalRulesetJar(): String? = ruleSetProviders.errorLoadingExternalRulesetJar
}
