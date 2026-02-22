package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.rule.engine.core.api.RuleInstanceProvider
import com.pinterest.ktlint.ruleset.standard.V1_3_0.StandardRuleSetProvider as StandardRuleSetProviderV1_3_0
import com.pinterest.ktlint.ruleset.standard.V1_3_1.StandardRuleSetProvider as StandardRuleSetProviderV1_3_1
import com.pinterest.ktlint.ruleset.standard.V1_4_1.StandardRuleSetProvider as StandardRuleSetProviderV1_4_1
import com.pinterest.ktlint.ruleset.standard.V1_5_0.StandardRuleSetProvider as StandardRuleSetProviderV1_5_0
import com.pinterest.ktlint.ruleset.standard.V1_6_0.StandardRuleSetProvider as StandardRuleSetProviderV1_6_0
import com.pinterest.ktlint.ruleset.standard.V1_7_2.StandardRuleSetProvider as StandardRuleSetProviderV1_7_2
import com.pinterest.ktlint.ruleset.standard.V1_8_0.StandardRuleSetProvider as StandardRuleSetProviderV1_8_0
import com.pinterest.ktlint.ruleset.standard.V1_8_1_SNAPSHOT.StandardRuleSetProvider as StandardRuleSetProviderV1_8_1_SNAPSHOT

enum class KtlintRulesetVersion(
    val ruleProviders: Set<RuleInstanceProvider>?,
    val alternativeRulesetVersion: KtlintRulesetVersion? = null,
) {
    // Versions should be ordered starting with default and then sorted from the most recent to the least recent version. All versions,
    // except DEFAULT, are associated with a specific version of the StandardRuleSetProvider (created via a relocation in the ShadowJar of
    // the ruleset subprojects in ktlint-lib). The version numbers should adhere to format `V1_2_3` or `V1_2_3_SNAPSHOT`.
    DEFAULT(null), // This version is linked to the latest (non-snapshot) version
    V1_8_1_SNAPSHOT(StandardRuleSetProviderV1_8_1_SNAPSHOT().getRuleProviders()),
    V1_8_0(StandardRuleSetProviderV1_8_0().getRuleProviders()),
    V1_7_2(StandardRuleSetProviderV1_7_2().getRuleProviders()),
    V1_7_1(StandardRuleSetProviderV1_7_2().getRuleProviders(), V1_7_2),
    V1_7_0(StandardRuleSetProviderV1_7_2().getRuleProviders(), V1_7_2),
    V1_6_0(StandardRuleSetProviderV1_6_0().getRuleProviders()),
    V1_5_0(StandardRuleSetProviderV1_5_0().getRuleProviders()),
    V1_4_1(StandardRuleSetProviderV1_4_1().getRuleProviders()),
    V1_4_0(StandardRuleSetProviderV1_4_1().getRuleProviders(), V1_4_1),
    V1_3_1(StandardRuleSetProviderV1_3_1().getRuleProviders()),
    V1_3_0(StandardRuleSetProviderV1_3_0().getRuleProviders()),

    // Older versions are not compatible with the plugin and are therefore not supported.
    // * Versions <= 1.2.x do not implement RuleAutoCorrectApproveHandler
    // * Version 0.50 uses mu/KotlinLogger which new minifying of the rulesets conflicts logger of 1.x versions
    // * Version 0.49 is incompatible as the RuleSet class was defined as value/data class which cannot be used from Java environment
    // * Version 0.48 and before use the RulesetProviderV2 instead of RulesetProviderV3
    ;

    fun ruleProviders() =
        ruleProviders
            ?: default.ruleProviders.orEmpty()

    fun label() =
        if (this == DEFAULT) {
            "Latest [${default.versionString()}] (recommended)"
        } else {
            versionString()
        }

    private fun versionString() =
        name
            .substringAfter("V")
            .replace("_SNAPSHOT", " (snapshot)")
            .replace("_", ".")

    companion object {
        fun findByLabelOrNull(label: String) = entries.firstOrNull { it.label() == label }

        fun findByLabelOrDefault(label: String) = findByLabelOrNull(label) ?: DEFAULT

        private val default =
            entries
                .filterNot { it.ruleProviders == null }
                .filterNot { it.name.endsWith("SNAPSHOT") }
                .map { ktlintRulesetVersion ->
                    ktlintRulesetVersion to
                        ktlintRulesetVersion
                            .versionString()
                            .split(".")
                            .joinToString(separator = ".") { it.format("%3d") }
                }.sortedBy { it.second }
                .reversed()
                .map { it.first }
                .first()
    }
}
