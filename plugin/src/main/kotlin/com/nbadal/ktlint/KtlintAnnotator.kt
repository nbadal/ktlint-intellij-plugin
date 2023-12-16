package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.lang.annotation.HighlightSeverity.WARNING
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.MANUAL
import com.nbadal.ktlint.KtlintFeature.DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.actions.ForceFormatIntention
import com.nbadal.ktlint.actions.KtlintModeIntention
import com.nbadal.ktlint.actions.KtlintRuleSuppressIntention
import com.nbadal.ktlint.actions.ShowAllKtlintViolationsIntention
import com.pinterest.ktlint.rule.engine.api.LintError

internal class KtlintAnnotator : ExternalAnnotator<List<LintError>, List<LintError>>() {
    override fun collectInformation(
        psiFile: PsiFile,
        editor: Editor,
        hasErrors: Boolean,
    ): List<LintError>? =
        when {
            hasErrors -> {
                // Ignore ktlint when other errors (for example compilation errors) are found
                null
            }

            psiFile.project.ktlintMode() == DISABLED -> {
                null
            }

            else -> {
                if (editor.document.ktlintAnnotatorUserData?.modificationTimestamp == editor.document.modificationStamp) {
                    // Document is unchanged since last time ktlint has run. Reuse lint errors from user data. It also has the advantage that
                    // a notification from the lint/format process in case on error is not displayed again.
                    println("Do not run ktlint as ktlintAnnotatorUserData has not changed on document ${psiFile.virtualFile.name}")
                    editor.document.ktlintAnnotatorUserData?.lintErrors
                } else {
                    ktlintLint(psiFile, "KtlintAnnotator")
                        .also { ktlintResult -> editor.document.setKtlintResult(ktlintResult) }
                        .lintErrors
                }
            }
        }

    override fun doAnnotate(collectedInfo: List<LintError>?): List<LintError>? =
        collectedInfo?.sortedWith(compareBy(LintError::line).thenComparingInt(LintError::col))

    override fun apply(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
    ) {
        val displayAllKtlintViolations = psiFile.project.isEnabled(DISPLAY_ALL_VIOLATIONS) || psiFile.displayAllKtlintViolations
        val ignoreViolationsPredicate: (LintError) -> Boolean =
            if (psiFile.project.config().ktlintMode == DISTRACT_FREE) {
                // By default, hide all violations which can be autocorrected unless the current editor is configured to display all
                // violations. Hiding aims to distract the developer as little as possible as those violations can be resolved by using
                // ktlint format. Showing all violations supports the developer in suppressing violations which may not be autocorrected.
                { lintError -> lintError.canBeAutoCorrected && !displayAllKtlintViolations }
            } else {
                { !displayAllKtlintViolations }
            }

        createAnnotationPerViolation(psiFile, errors, annotationHolder, ignoreViolationsPredicate)
        createAnnotationSummaryForIgnoredViolations(psiFile, errors, annotationHolder, ignoreViolationsPredicate)
    }

    private fun createAnnotationPerViolation(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
        shouldIgnore: (LintError) -> Boolean,
    ) {
        errors
            ?.filterNot { shouldIgnore(it) }
            ?.forEach { lintError ->
                errorTextRange(psiFile, lintError)
                    ?.let { errorTextRange ->
                        annotationHolder
                            .newAnnotation(ERROR, lintError.errorMessage())
                            .range(errorTextRange)
                            .withFix(KtlintRuleSuppressIntention(lintError))
                            .withFix(KtlintModeIntention(MANUAL))
                            .create()
                    }
            }
    }

    private fun createAnnotationSummaryForIgnoredViolations(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
        shouldIgnore: (LintError) -> Boolean,
    ) {
        val countCanBeAutoCorrected =
            errors
                ?.filter { shouldIgnore(it) }
                ?.count { it.canBeAutoCorrected } ?: 0
        val countCanNotBeAutoCorrected =
            errors
                ?.filter { shouldIgnore(it) }
                ?.count { !it.canBeAutoCorrected } ?: 0
        val message =
            when {
                countCanBeAutoCorrected > 0 && countCanNotBeAutoCorrected > 0 -> {
                    "Ktlint found ${countCanBeAutoCorrected + countCanNotBeAutoCorrected} lint violations of which " +
                        "$countCanBeAutoCorrected can be autocorrected"
                }

                countCanBeAutoCorrected > 0 -> {
                    "Ktlint found $countCanBeAutoCorrected violations which can be autocorrected"
                }

                countCanNotBeAutoCorrected > 0 -> {
                    "Ktlint found $countCanNotBeAutoCorrected violations that should be corrected manually"
                }

                else -> {
                    return
                }
            }

        if (psiFile.project.isEnabled(KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND)) {
            annotationHolder
                .newAnnotation(INFORMATION, message)
                .fileLevel()
                .withFix(KtlintModeIntention(DISTRACT_FREE))
                .withFix(KtlintModeIntention(MANUAL))
                .withFix(KtlintModeIntention(DISABLED))
                .create()
        } else {
            annotationHolder
                .newAnnotation(WARNING, message)
                .range(TextRange(0, 0))
                .withFix(ShowAllKtlintViolationsIntention())
                .withFix(ForceFormatIntention())
                .create()
        }
    }

    private val PsiFile.displayAllKtlintViolations
        get() =
            viewProvider
                .document
                .ktlintAnnotatorUserData
                .also {
                    println("Annotator: $it")
                }?.displayAllKtlintViolations ?: false

    private fun LintError.errorMessage(): String = "$detail (${ruleId.value})"

    private fun errorTextRange(
        psiFile: PsiFile,
        lintError: LintError,
    ): TextRange? {
        val document = psiFile.viewProvider.document!!
        return if (document.textLength == 0) {
            // It is not possible to draw an annotation on empty file
            null
        } else {
            psiFile
                .findElementAt(lintError.offsetFromStartOf(document))
                ?.let { TextRange.from(it.startOffset, it.textLength) }
                ?: TextRange(lintError.lineStartOffset(document), lintError.getLineEndOffset(document))
        }
    }

    private fun LintError.offsetFromStartOf(document: Document) =
        with(document) {
            val lineStartOffset = lineStartOffset(this)
            (lineStartOffset + (col - 1))
                .coerceIn(lineStartOffset, textLength)
        }

    private fun LintError.lineStartOffset(document: Document) =
        with(document) {
            getLineStartOffset((line - 1).coerceIn(0, lineCount - 1))
                .coerceIn(0, textLength)
        }

    private fun LintError.getLineEndOffset(document: Document) =
        with(document) {
            getLineEndOffset((line - 1).coerceIn(0, lineCount - 1))
                .coerceIn(0, textLength)
        }
}

fun Project.resetKtlintAnnotator() {
    // Reset KtlintRuleEngine as it has cached the '.editorconfig'
    config().resetKtlintRuleEngine()

    // Remove user data from all open documents
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
