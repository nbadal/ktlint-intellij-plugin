package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.pinterest.ktlint.rule.engine.api.LintError

private val ktlintAnnotatorUserDataKey = Key<KtlintAnnotatorUserData>("ktlint-annotator")

/**
 * Metadata for displaying the Ktlint Annotator. The metadata is stored as user data on the document.
 */
internal data class KtlintAnnotatorUserData(
    val modificationTimestamp: Long,
    val lintErrors: List<LintError>,
    val displayAllKtlintViolations: Boolean,
)

internal val Document.ktlintAnnotatorUserData
    get() = getUserData(ktlintAnnotatorUserDataKey)

internal fun Document.removeKtlintAnnotatorUserData() = putUserData(ktlintAnnotatorUserDataKey, null)

internal fun Document.setKtlintResult(ktlintResult: KtlintRuleEngineWrapper.KtlintResult) {
    val currentUserData = getUserData(ktlintAnnotatorUserDataKey)
    putUserData(
        ktlintAnnotatorUserDataKey,
        KtlintAnnotatorUserData(
            modificationTimestamp = modificationStamp,
            lintErrors = ktlintResult.lintErrors,
            displayAllKtlintViolations = currentUserData?.displayAllKtlintViolations ?: false,
        ),
    )
}

internal fun Document.setDisplayAllKtlintViolations() {
    val currentUserData = getUserData(ktlintAnnotatorUserDataKey)
    putUserData(
        ktlintAnnotatorUserDataKey,
        KtlintAnnotatorUserData(
            modificationTimestamp = modificationStamp,
            lintErrors = currentUserData?.lintErrors ?: emptyList(),
            displayAllKtlintViolations = true,
        ),
    )
}
