package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.nbadal.ktlint.connector.LintError

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

internal fun Project.resetKtlintAnnotatorUserData() {
    // Remove user data from all open documents in the project
    FileEditorManager
        .getInstance(this)
        .openFiles
        .forEach { virtualFile ->
            FileDocumentManager
                .getInstance()
                .getDocument(virtualFile)
                ?.removeKtlintAnnotatorUserData()
        }

    // Restart code analyzer so that open files are scanned again
    DaemonCodeAnalyzer.getInstance(this).restart()
}

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
