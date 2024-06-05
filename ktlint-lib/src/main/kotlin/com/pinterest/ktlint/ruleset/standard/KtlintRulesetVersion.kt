package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.ruleset.standard.V0_50_0.StandardRuleSetProvider as StandardRuleSetProviderV0_50_0
import com.pinterest.ktlint.ruleset.standard.V1_00_1.StandardRuleSetProvider as StandardRuleSetProviderV1_00_1
import com.pinterest.ktlint.ruleset.standard.V1_01_1.StandardRuleSetProvider as StandardRuleSetProviderV1_01_1
import com.pinterest.ktlint.ruleset.standard.V1_02_0.StandardRuleSetProvider as StandardRuleSetProviderV1_02_0
import com.pinterest.ktlint.ruleset.standard.V1_02_1.StandardRuleSetProvider as StandardRuleSetProviderV1_02_1
import com.pinterest.ktlint.ruleset.standard.V1_03_0.StandardRuleSetProvider as StandardRuleSetProviderV1_03_0

/**
 * Policies for supporting rulesets from older versions:
 *   * Only support for RuleSetProviderV3
 *   * Only latest patch version of a minor release is supported
 */
enum class KtlintRulesetVersion(
    /**
     * Label should match with the dropdown values of the ktlint version field in the configuration panel "KtlintConfigForm".
     */
    val label: String,
    private val ruleSetProvider: RuleSetProviderV3?,
) {
    // Versions should be ordered starting with default and then sorted from the most recent to the least recent version
    DEFAULT("default (recommended)", null),
    V1_3_0("1.3.0", StandardRuleSetProviderV1_03_0()),
    V1_2_1("1.2.1", StandardRuleSetProviderV1_02_1()),
    V1_2_0("1.2.0", StandardRuleSetProviderV1_02_0()),
    V1_1_1("1.1.1", StandardRuleSetProviderV1_01_1()),
    V1_0_1("1.0.1", StandardRuleSetProviderV1_00_1()),
    V0_50_0("0.50.0", StandardRuleSetProviderV0_50_0()),

    // Older versions are not compatible with the plugin and are therefore not supported.
    // * Version 0.49 is incompatible as the RuleSet class was defined as value/data class which can not be used from Java environment
    // * Version 0.48 and before use the RulesetProviderV2 instead of RulesetProviderV3
    ;

    fun ruleProviders() =
        ruleSetProvider?.getRuleProviders()
            ?: default.ruleSetProvider?.getRuleProviders().orEmpty()

    /**
     * Check whether the current rule set version is released before the given version. False in case the current release equals the given
     * release, or in case it is released after the given release.
     */
    fun isReleasedBefore(otherKtlintRulesetVersion: KtlintRulesetVersion): Boolean =
        // Default version (ordinal 0) is equal to the most recent released version (ordinal 1) are the same version. So the current version
        // can never before any other version. Higher ordinals are older versions.
        ordinal != 0 && ordinal > otherKtlintRulesetVersion.ordinal

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
