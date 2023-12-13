package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.Key
import com.nbadal.ktlint.KtlintAnnotatorUserData.KtlintStatus.SUCCESS

private val ktlintAnnotatorUserDataKey = Key<KtlintAnnotatorUserData>("ktlint-annotator")

/**
 * Metadata for displaying the Ktlint Annotator. The metadata is stored as user data on the document.
 */
internal data class KtlintAnnotatorUserData(
    val modificationTimestamp: Long,
    val ktlintStatus: KtlintStatus,
    val displayAllKtlintViolations: Boolean,
) {
    enum class KtlintStatus { FAILURE, SUCCESS }
}

internal val Document.ktlintAnnotatorUserData
    get() = getUserData(ktlintAnnotatorUserDataKey)

internal fun Document.removeKtlintAnnotatorUserData() = putUserData(ktlintAnnotatorUserDataKey, null)

internal fun Document.setKtlintStatus(ktlintStatus: KtlintAnnotatorUserData.KtlintStatus) {
    val currentUserData = getUserData(ktlintAnnotatorUserDataKey)
    putUserData(
        ktlintAnnotatorUserDataKey,
        KtlintAnnotatorUserData(
            modificationTimestamp = modificationStamp,
            ktlintStatus = ktlintStatus,
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
            ktlintStatus = currentUserData?.ktlintStatus ?: SUCCESS,
            displayAllKtlintViolations = true,
        ),
    )
}
