package com.nbadal.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import shadow.com.pinterest.ktlint.ruleset.standard.StandardRuleSetProviderV0_50_0
import shadow.com.pinterest.ktlint.ruleset.standard.StandardRuleSetProviderV1_00_1
import shadow.com.pinterest.ktlint.ruleset.standard.StandardRuleSetProviderV1_01_1
import shadow.com.pinterest.ktlint.ruleset.standard.StandardRuleSetProviderV1_02_1

/**
 * Policies for supporting rulesets from older versions:
 *   * Only support for RuleSetProviderV3
 *   * Only latest patch version of a minor release is supported
 */
enum class KtlintRulesetVersion(
    val label: String,
    private val ruleSetProvider: RuleSetProviderV3?,
) {
    DEFAULT("default (recommended)", null),
    V1_2_1("1.2.1", StandardRuleSetProviderV1_02_1()),
    V1_1_1("1.1.1", StandardRuleSetProviderV1_01_1()),
    V1_0_1("1.0.1", StandardRuleSetProviderV1_00_1()),
    V0_50_0("0.50.0", StandardRuleSetProviderV0_50_0()),

    // Older versions are not compatible with the plugin and are therefore not supported.
    // * V49 is incompatible as the RuleSet class was defined as value/data class which can not be used from Java environment
    // * V48 and before use the RulesetProviderV2 instead of RulesetProviderV3

    // User must at least import one custom ruleset as ktlint plugin otherwise will throw errors
    NONE("none", null),
    ;

    fun ruleProviders() =
        ruleSetProvider?.getRuleProviders()
            ?: default.ruleSetProvider?.getRuleProviders().orEmpty()

    companion object {
        fun findByLabelOrDefault(label: String) = entries.firstOrNull { it.label == label } ?: DEFAULT

        private val default =
            entries
                .filterNot { it == DEFAULT }
                .map { ktlintRulesetVersion ->
                    ktlintRulesetVersion to
                        ktlintRulesetVersion
                            .label
                            .split(".")
                            .joinToString(separator = ".") { it.format("%3d") }
                }.sortedBy { it.second }
                .reversed()
                .map { it.first }
                .first()
    }
}
