package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.ruleset.standard.V1_0_1.StandardRuleSetProvider as StandardRuleSetProviderV1_0_1
import com.pinterest.ktlint.ruleset.standard.V1_1_1.StandardRuleSetProvider as StandardRuleSetProviderV1_1_1
import com.pinterest.ktlint.ruleset.standard.V1_2_0.StandardRuleSetProvider as StandardRuleSetProviderV1_2_0
import com.pinterest.ktlint.ruleset.standard.V1_2_1.StandardRuleSetProvider as StandardRuleSetProviderV1_2_1
import com.pinterest.ktlint.ruleset.standard.V1_3_0.StandardRuleSetProvider as StandardRuleSetProviderV1_3_0
import com.pinterest.ktlint.ruleset.standard.V1_3_1.StandardRuleSetProvider as StandardRuleSetProviderV1_3_1
import com.pinterest.ktlint.ruleset.standard.V1_4_1.StandardRuleSetProvider as StandardRuleSetProviderV1_4_1

/**
 * Policies for supporting rulesets from older versions:
 *   * Only support for RuleSetProviderV3
 *   * Only latest patch version of a minor release is supported
 */
enum class KtlintRulesetVersion(
    /**
     * Labels are displayed in the ktlint settings screen
     */
    val label: String,
    private val ruleSetProvider: RuleSetProviderV3?,
) {
    // Versions should be ordered starting with default and then sorted from the most recent to the least recent version
    DEFAULT("default (recommended)", null),

    // The latest released version of Ktlint is to be loaded via the "StandardRuleSetProvider()" constructor. So whenever adding a new
    // release, a new ruleset subproject has to be created for the previous release.
    V1_5_0("1.5.0", StandardRuleSetProvider()),

    // For each older release that is supported, a separate ruleset subproject exists in which the StandardRuleSetProvider is relocated to
    // a unique class name.
    V1_4_1("1.4.1", StandardRuleSetProviderV1_4_1()),
    V1_3_1("1.3.1", StandardRuleSetProviderV1_3_1()),
    V1_3_0("1.3.0", StandardRuleSetProviderV1_3_0()),
    V1_2_1("1.2.1", StandardRuleSetProviderV1_2_1()),
    V1_2_0("1.2.0", StandardRuleSetProviderV1_2_0()),
    V1_1_1("1.1.1", StandardRuleSetProviderV1_1_1()),
    V1_0_1("1.0.1", StandardRuleSetProviderV1_0_1()),

    // Older versions are not compatible with the plugin and are therefore not supported.
    // * Version 0.50 uses mu/KotlinLogger which new minifying of the rulesets conflicts logger of 1.x versions
    // * Version 0.49 is incompatible as the RuleSet class was defined as value/data class which cannot be used from Java environment
    // * Version 0.48 and before use the RulesetProviderV2 instead of RulesetProviderV3
    ;

    fun ruleProviders() =
        ruleSetProvider?.getRuleProviders()
            ?: default.ruleSetProvider?.getRuleProviders().orEmpty()

    companion object {
        fun findByLabelOrDefault(label: String) = entries.firstOrNull { it.label == label } ?: DEFAULT

        private val default =
            entries
                .filterNot { it.ruleSetProvider == null }
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
