package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.nbadal.ktlint.actions.KtlintRuleSuppressIntention
import com.pinterest.ktlint.rule.engine.api.LintError

class KtlintAnnotator : ExternalAnnotator<KtlintFormatResult, List<LintError>>() {
    override fun collectInformation(
        psiFile: PsiFile,
        editor: Editor,
        hasErrors: Boolean,
    ): KtlintFormatResult? =
        if (hasErrors) {
            null
        } else {
            ktlintLint(psiFile, "KtlintAnnotator")
        }

    override fun doAnnotate(collectedInfo: KtlintFormatResult?): List<LintError>? =
        // Ignore errors that can be autocorrected by ktlint to prevent that developer is going to resolve errors
        // manually and errors in baseline.
        collectedInfo?.canNotBeAutoCorrectedErrors

    override fun apply(
        file: PsiFile,
        errors: List<LintError>?,
        holder: AnnotationHolder,
    ) {
        errors?.forEach { lintError ->
            holder
                .newAnnotation(HighlightSeverity.ERROR, lintError.errorMessage())
                .apply {
                    range(errorTextRange(file, lintError))

                    if (!lintError.canBeAutoCorrected) {
                        withFix(KtlintRuleSuppressIntention(lintError))
                    }

                    create()
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
