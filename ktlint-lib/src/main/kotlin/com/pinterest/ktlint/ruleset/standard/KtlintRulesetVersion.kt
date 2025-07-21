package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.ruleset.standard.V1_0_1.StandardRuleSetProvider as StandardRuleSetProviderV1_0_1
import com.pinterest.ktlint.ruleset.standard.V1_1_1.StandardRuleSetProvider as StandardRuleSetProviderV1_1_1
import com.pinterest.ktlint.ruleset.standard.V1_2_0.StandardRuleSetProvider as StandardRuleSetProviderV1_2_0
import com.pinterest.ktlint.ruleset.standard.V1_2_1.StandardRuleSetProvider as StandardRuleSetProviderV1_2_1
import com.pinterest.ktlint.ruleset.standard.V1_3_0.StandardRuleSetProvider as StandardRuleSetProviderV1_3_0
import com.pinterest.ktlint.ruleset.standard.V1_3_1.StandardRuleSetProvider as StandardRuleSetProviderV1_3_1
import com.pinterest.ktlint.ruleset.standard.V1_4_1.StandardRuleSetProvider as StandardRuleSetProviderV1_4_1
import com.pinterest.ktlint.ruleset.standard.V1_5_0.StandardRuleSetProvider as StandardRuleSetProviderV1_5_0
import com.pinterest.ktlint.ruleset.standard.V1_6_0.StandardRuleSetProvider as StandardRuleSetProviderV1_6_0
import com.pinterest.ktlint.ruleset.standard.V1_7_0.StandardRuleSetProvider as StandardRuleSetProviderV1_7_0
import com.pinterest.ktlint.ruleset.standard.V1_7_1.StandardRuleSetProvider as StandardRuleSetProviderV1_7_1

/**
 * Policies for supporting rulesets from older versions:
 *   * Only support for RuleSetProviderV3
 *   * Only latest patch version of a minor release is supported
 */
enum class KtlintRulesetVersion(
    val ruleSetProvider: RuleSetProviderV3?,
) {
    // Versions should be ordered starting with default and then sorted from the most recent to the least recent version. All versions,
    // except DEFAULT, are associated with a specific version of the StandardRuleSetProvider (created via a relocation in the ShadowJar of
    // the ruleset subprojects in ktlint-lib). The version numbers should adhere to format `V1_2_3` or `V1_2_3_SNAPSHOT`.
    DEFAULT(null), // This version is linked to the latest (non-snapshot) version
    V1_7_1(StandardRuleSetProviderV1_7_1()),
    V1_7_0(StandardRuleSetProviderV1_7_0()),
    V1_6_0(StandardRuleSetProviderV1_6_0()),
    V1_5_0(StandardRuleSetProviderV1_5_0()),
    V1_4_1(StandardRuleSetProviderV1_4_1()),
    V1_3_1(StandardRuleSetProviderV1_3_1()),
    V1_3_0(StandardRuleSetProviderV1_3_0()),
    V1_2_1(StandardRuleSetProviderV1_2_1()),
    V1_2_0(StandardRuleSetProviderV1_2_0()),
    V1_1_1(StandardRuleSetProviderV1_1_1()),
    V1_0_1(StandardRuleSetProviderV1_0_1()),

    // Older versions are not compatible with the plugin and are therefore not supported.
    // * Version 0.50 uses mu/KotlinLogger which new minifying of the rulesets conflicts logger of 1.x versions
    // * Version 0.49 is incompatible as the RuleSet class was defined as value/data class which cannot be used from Java environment
    // * Version 0.48 and before use the RulesetProviderV2 instead of RulesetProviderV3
    ;

    fun ruleProviders() =
        ruleSetProvider?.getRuleProviders()
            ?: default.ruleSetProvider?.getRuleProviders().orEmpty()

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
        fun findByLabelOrDefault(label: String) = entries.firstOrNull { it.label() == label } ?: DEFAULT

        private val default =
            entries
                .filterNot { it.ruleSetProvider == null }
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
