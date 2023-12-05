package com.nbadal.ktlint

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key

private val ktlintAnnotatorUserDataKey = Key<KtlintAnnotatorUserData>("ktlint-annotator")

/**
 *
 */
internal data class KtlintAnnotatorUserData(
    val editorHashCode: Int,
    val modificationTimestamp: Long,
    val ktlintStatus: KtlintStatus,
) {
    enum class KtlintStatus { FAILURE, SUCCESS }
}

internal fun Editor.hasStatus(ktlintStatus: KtlintAnnotatorUserData.KtlintStatus) =
    getKtlintAnnotatorUserData()
        .takeIf { ktlintAnnotatorUserData ->
            ktlintAnnotatorUserData?.hashCode() == hashCode() &&
                ktlintAnnotatorUserData.modificationTimestamp == document.modificationStamp
        }?.ktlintStatus == ktlintStatus

private fun Editor.getKtlintAnnotatorUserData() = document.getUserData(ktlintAnnotatorUserDataKey)

internal fun Editor.updateKtlintStatus(ktlintStatus: KtlintAnnotatorUserData.KtlintStatus) =
    document.putUserData(
        ktlintAnnotatorUserDataKey,
        with(this) {
            KtlintAnnotatorUserData(hashCode(), document.modificationStamp, ktlintStatus)
        },
    )
