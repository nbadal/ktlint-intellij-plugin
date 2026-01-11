package com.nbadal.ktlint

import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.ruleset.standard.KtlintRulesetVersion

open class KtlintRuleEngineProvider {
    /**
     * The set of ruleset providers that are loaded into the KtLintRuleEngine
     */
    private lateinit var ruleSetProviders: RuleSetProviders

    private lateinit var _ktlintRuleEngine: KtLintRuleEngine

    val ktlintRuleEngine: KtLintRuleEngine
        get() = _ktlintRuleEngine

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
            _ktlintRuleEngine =
                KtLintRuleEngine(
                    editorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
                    ruleProviders = ruleSetProviders.ruleProviders,
                )
        }
    }

    fun errorLoadingExternalRulesetJar(): String? = ruleSetProviders.errorLoadingExternalRulesetJar
}
