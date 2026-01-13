package com.nbadal.ktlint

import com.nbadal.ktlint.KtlintRulesetVersion
import java.io.File

data class RuleSetProviders(
    val ktlintRulesetVersion: KtlintRulesetVersion,
    val externalJarPaths: List<String>,
) {
    private var _errorLoadingExternalRulesetJar: String? = null

    val errorLoadingExternalRulesetJar: String?
        get() = _errorLoadingExternalRulesetJar

    val ruleProviders =
        externalJarPaths
            .flatMap { externalJarRuleProviders(it) }
            .plus(standardKtlintRuleProviders())
            .toSet()

    private fun externalJarRuleProviders(path: String) =
        try {
            listOf(File(path).toURI().toURL())
                .loadCustomRuleProviders()
                .also { ktlintLibLogger.info { "Loaded ${it.size} rules from custom rule provider $path" } }
        } catch (throwable: Throwable) {
            ktlintLibLogger.error(throwable) { "Cannot load external ruleset jar '$path" }
            // It is not possible to direct call KtlintNotifier to display a notification. This results in endless loop while trying to load
            // the settings dialog
            _errorLoadingExternalRulesetJar =
                "An error occurred while reading external ruleset file '$path'. No ktlint ruleset can be loaded from this file."
            emptyList()
        }

    private fun standardKtlintRuleProviders() =
        ktlintRulesetVersion
            .ruleProviders()
            .also {
                ktlintLibLogger.info {
                    "Loaded ${ktlintRulesetVersion.ruleProviders().size} rules from default ktlint ruleset version '${ktlintRulesetVersion.label()}'"
                }
            }
}
