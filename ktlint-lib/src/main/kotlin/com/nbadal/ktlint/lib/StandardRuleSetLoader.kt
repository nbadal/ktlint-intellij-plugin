package com.nbadal.ktlint.lib

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider

class StandardRuleSetLoader {
    private lateinit var ktlintRulesetVersion: KtlintRulesetVersion
    private lateinit var ruleProviders: Set<RuleProvider>

    fun loadRuleProviders(ktlintRulesetVersion: KtlintRulesetVersion): Set<RuleProvider> {
        if (!::ruleProviders.isInitialized || this.ktlintRulesetVersion != ktlintRulesetVersion) {
            this.ktlintRulesetVersion = ktlintRulesetVersion
            ruleProviders =
                ktlintRulesetVersion
                    .ruleProviders()
                    .also {
                        ktlintLibLogger.info {
                            "Loaded ${ktlintRulesetVersion.ruleProviders().size} rules from default ktlint ruleset version '${ktlintRulesetVersion.label()}'"
                        }
                    }
        }
        return ruleProviders
    }
}
