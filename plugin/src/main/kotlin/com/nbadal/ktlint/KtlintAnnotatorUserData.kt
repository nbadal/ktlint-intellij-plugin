package com.nbadal.ktlint

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.nbadal.ktlint.KtlintAnnotatorUserData.KtlintStatus.SUCCESS

private val ktlintAnnotatorUserDataKey = Key<KtlintAnnotatorUserData>("ktlint-annotator")

/**
 *
 */
internal data class KtlintAnnotatorUserData(
    val editorHashCode: Int,
    val modificationTimestamp: Long,
    val ktlintStatus: KtlintStatus,
    val displayAllKtlintViolations: Boolean,
) {
    enum class KtlintStatus { FAILURE, SUCCESS }
}

internal fun Editor.hasStatus(ktlintStatus: KtlintAnnotatorUserData.KtlintStatus) =
    getKtlintAnnotatorUserData()
        .takeIf { ktlintAnnotatorUserData ->
            ktlintAnnotatorUserData?.hashCode() == hashCode() &&
                ktlintAnnotatorUserData.modificationTimestamp == document.modificationStamp
        }?.ktlintStatus == ktlintStatus

private fun Editor.getKtlintAnnotatorUserData() = document.getKtlintAnnotatorUserData()

internal fun Document.getKtlintAnnotatorUserData() = getUserData(ktlintAnnotatorUserDataKey)

internal fun Editor.updateKtlintStatus(ktlintStatus: KtlintAnnotatorUserData.KtlintStatus) =
    with(document) {
        val currentUserData = getUserData(ktlintAnnotatorUserDataKey)
        putUserData(
            ktlintAnnotatorUserDataKey,
            KtlintAnnotatorUserData(
                editorHashCode = hashCode(),
                modificationTimestamp = document.modificationStamp,
                ktlintStatus = ktlintStatus,
                displayAllKtlintViolations = currentUserData?.displayAllKtlintViolations ?: false,
            ),
        )
    }

internal fun Editor.displayAllKtlintViolations(displayAll: Boolean) =
    with(document) {
        val currentUserData = getUserData(ktlintAnnotatorUserDataKey)
        putUserData(
            ktlintAnnotatorUserDataKey,
            KtlintAnnotatorUserData(
                editorHashCode = hashCode(),
                modificationTimestamp = document.modificationStamp,
                ktlintStatus = currentUserData?.ktlintStatus ?: SUCCESS,
                displayAllKtlintViolations = displayAll,
            ),
        )
    }
