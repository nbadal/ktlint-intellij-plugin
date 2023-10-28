package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
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
        file: PsiFile,
        it: LintError,
    ): TextRange {
        val doc = file.viewProvider.document!!
        val lineStart = doc.getLineStartOffset((it.line - 1).coerceIn(0, doc.lineCount - 1)).coerceIn(0, doc.textLength)
        val errorOffset = (lineStart + (it.col - 1)).coerceIn(lineStart, doc.textLength)

        // Full line range in case we can't discern the indicated element:
        val lineEndOffset = doc.getLineEndOffset((it.line - 1).coerceIn(0, doc.lineCount - 1)).coerceIn(0, doc.textLength)
        val fullLineRange = TextRange(lineStart, lineEndOffset)

        return file.findElementAt(errorOffset)?.let { TextRange.from(errorOffset, it.textLength) } ?: fullLineRange
    }
}
