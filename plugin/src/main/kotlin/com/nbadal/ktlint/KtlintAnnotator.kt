package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.intellij.lang.annotation.HighlightSeverity.WARNING
import com.intellij.lang.annotation.HighlightSeverity.WEAK_WARNING
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.MANUAL
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
        if (hasErrors) {
            null
        } else {
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

    override fun doAnnotate(collectedInfo: List<LintError>?): List<LintError>? =
        collectedInfo?.sortedWith(compareBy(LintError::line).thenComparingInt(LintError::col))

    override fun apply(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
    ) {
        if (psiFile.project.config().ktlintMode == DISTRACT_FREE) {
            applyWhenPluginIsEnabled(psiFile, errors, annotationHolder)
        } else {
            applyWhenPluginIsNotEnabled(psiFile, errors, annotationHolder)
        }
    }

    private fun applyWhenPluginIsEnabled(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
    ) {
        // Showing all errors which can be autocorrected is distracting, and can lead to waste of developer time in case the developer
        // starts fixing the errors manually. So display a warning with the number of errors that can be autocorrected.
        errors
            ?.count { it.canBeAutoCorrected }
            ?.takeIf { it > 0 }
            ?.let { count ->
                annotationHolder
                    .newAnnotation(WARNING, "This file contains $count lint violations which can be autocorrected by ktlint")
                    .range(TextRange(0, 0))
                    .withFix(ForceFormatIntention())
                    .create()
            }

        errors
            ?.filter {
                // By default, hide all violations which can be autocorrected unless the current editor is configured to display all
                // violations. Hiding aims to distract the developer as little as possible as those violations can be resolved by using
                // ktlint format. Showing all violations supports the developer in suppressing violations which may not be autocorrected.
                !it.canBeAutoCorrected || psiFile.displayAllKtlintViolations
            }?.forEach { lintError ->
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

    private fun applyWhenPluginIsNotEnabled(
        psiFile: PsiFile,
        errors: List<LintError>?,
        annotationHolder: AnnotationHolder,
    ) {
        if (psiFile.displayAllKtlintViolations) {
            errors
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
        } else {
            val countCanBeAutoCorrected = errors?.count { it.canBeAutoCorrected } ?: 0
            val countCanNotBeAutoCorrected = errors?.count { !it.canBeAutoCorrected } ?: 0
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

            val annotationBuilder =
                annotationHolder
                    .newAnnotation(WEAK_WARNING, message)
                    .range(TextRange(0, 0))
                    .withFix(ShowAllKtlintViolationsIntention())
                    .withFix(ForceFormatIntention())
                    .withFix(KtlintModeIntention(DISTRACT_FREE))
            if (psiFile.project.isEnabled(KtlintFeature.AUTOMATICALLY_DISPLAY_BANNER_WITH_NUMBER_OF_VIOLATIONS_FOUND)) {
                annotationBuilder
                    .fileLevel()
                    .withFix(KtlintModeIntention(MANUAL))
            }
            annotationBuilder.create()
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
