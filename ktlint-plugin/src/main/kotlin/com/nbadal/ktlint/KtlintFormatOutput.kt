package com.nbadal.ktlint

import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.SUCCESS

/**
 * The outcome of ktlint format before it is applied to the document. The [formattedCode] is kept out of the [KtlintResult] so that it is
 * only retained until it has been written to the document.
 */
internal class KtlintFormatOutput(
    val ktlintResult: KtlintResult,
    /**
     * The formatted code when it differs from the code that was provided to ktlint, or null when there is nothing to apply.
     */
    val formattedCode: String? = null,
) {
    init {
        require(formattedCode == null || ktlintResult.status == SUCCESS) {
            "Formatted code can only be provided for a successful ktlint run, but status is ${ktlintResult.status}"
        }
    }

    /**
     * The result to report after the [formattedCode] has been written to the document.
     */
    fun appliedKtlintResult() = ktlintResult.copy(fileChangedByFormat = formattedCode != null)
}
