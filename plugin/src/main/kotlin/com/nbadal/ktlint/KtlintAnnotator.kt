package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.intentions.DisablePluginIntention
import com.nbadal.ktlint.intentions.FormatIntention
import com.nbadal.ktlint.intentions.GlobalDisableRuleIntention
import com.nbadal.ktlint.intentions.LineDisableIntention
import com.pinterest.ktlint.core.LintError

class KtlintAnnotator : ExternalAnnotator<PsiFile, List<LintError>>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): List<LintError> {
        val config = file.project.config()
        if (!config.enableKtlint || config.hideErrors) {
            return emptyList()
        }

        return doLint(file, config, false).uncorrectedErrors
    }

    override fun apply(file: PsiFile, errors: List<LintError>, holder: AnnotationHolder) {
        val config = file.project.config()

        errors.forEach {
            val errorRange = errorTextRange(file, it)
            val message = "${it.detail} (${it.ruleId})"
            val severity = if (config.treatAsErrors) HighlightSeverity.ERROR else HighlightSeverity.WARNING

            holder.createAnnotation(severity, errorRange, message).apply {
                if (it.canBeAutoCorrected) registerFix(FormatIntention())
                registerFix(GlobalDisableRuleIntention(it.ruleId))
                registerFix(LineDisableIntention(it))
                registerFix(DisablePluginIntention())
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
