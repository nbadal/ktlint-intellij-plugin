package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.ENABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.NOT_INITIALIZED
import com.nbadal.ktlint.actions.KtlintModeIntention
import com.nbadal.ktlint.actions.KtlintRuleSuppressIntention
import com.pinterest.ktlint.rule.engine.api.LintError

class KtlintAnnotator : ExternalAnnotator<List<LintError>, List<LintError>>() {
    override fun collectInformation(
        psiFile: PsiFile,
        editor: Editor,
        hasErrors: Boolean,
    ): List<LintError>? =
        if (hasErrors) {
            null
        } else {
            ktlintLint(psiFile, "KtlintAnnotator")
        }

    override fun doAnnotate(collectedInfo: List<LintError>?): List<LintError>? =
        collectedInfo?.sortedWith(compareBy(LintError::line).thenComparingInt(LintError::col))

    override fun apply(
        file: PsiFile,
        errors: List<LintError>?,
        holder: AnnotationHolder,
    ) {
        when (file.project.config().ktlintMode) {
            NOT_INITIALIZED -> {
                errors?.forEach { lintError ->
                    holder
                        .newAnnotation(HighlightSeverity.WARNING, lintError.errorMessage())
                        .range(errorTextRange(file, lintError))
                        .withFix(KtlintModeIntention(ENABLED))
                        .withFix(KtlintModeIntention(DISABLED))
                        .create()
                }
            }

            ENABLED -> {
                errors?.forEach { lintError ->
                    holder
                        .newAnnotation(HighlightSeverity.ERROR, lintError.errorMessage())
                        .range(errorTextRange(file, lintError))
                        .withFix(KtlintRuleSuppressIntention(lintError))
                        .withFix(KtlintModeIntention(DISABLED))
                        .create()
                }
            }

            DISABLED -> {
                errors?.forEach { lintError ->
                    holder
                        .newAnnotation(HighlightSeverity.WEAK_WARNING, lintError.errorMessage())
                        .range(errorTextRange(file, lintError))
                        .withFix(KtlintModeIntention(ENABLED))
                        .create()
                }
            }
        }
    }

    private fun LintError.errorMessage(): String = "$detail (${ruleId.value})"

    private fun errorTextRange(
        psiFile: PsiFile,
        lintError: LintError,
    ): TextRange {
        val document = psiFile.viewProvider.document!!
        return psiFile
            .findElementAt(lintError.offsetFromStartOf(document))
            ?.let { TextRange.from(it.startOffset, it.textLength) }
            ?: TextRange(lintError.lineStartOffset(document), lintError.getLineEndOffset(document))
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
