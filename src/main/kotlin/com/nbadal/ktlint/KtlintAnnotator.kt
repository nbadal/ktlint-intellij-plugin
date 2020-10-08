package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.LintError

class KtlintAnnotator : ExternalAnnotator<PsiFile, List<LintError>>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): List<LintError>? {
        val config = KtlintConfigStorage.instance(file.project)
        if (!config.enableKtlint) {
            return emptyList()
        }

        return doLint(file, config, false).uncorrectedErrors
    }

    override fun apply(file: PsiFile, errors: List<LintError>, holder: AnnotationHolder) {
        val config = KtlintConfigStorage.instance(file.project)

        errors.forEach {
            val errorRange = errorTextRange(file, it)
            val message = "${it.detail} (${it.ruleId})"
            val severity = if (config.treatAsErrors) HighlightSeverity.ERROR else HighlightSeverity.WARNING

            holder.createAnnotation(severity, errorRange, message).apply {
                registerFix(KtlintFormatAction())
                registerFix(KtlintConfigAction())
            }
        }
    }

    private fun errorTextRange(file: PsiFile, it: LintError): TextRange {
        val doc = file.viewProvider.document!!
        val lineStart = doc.getLineStartOffset(it.line - 1)
        val errorOffset = lineStart + (it.col - 1)

        // Full line range in case we can't discern the indicated element:
        val fullLineRange = TextRange(lineStart, doc.getLineEndOffset(it.line - 1))

        return file.findElementAt(errorOffset)?.let { TextRange.from(errorOffset, it.textLength) } ?: fullLineRange
    }
}
