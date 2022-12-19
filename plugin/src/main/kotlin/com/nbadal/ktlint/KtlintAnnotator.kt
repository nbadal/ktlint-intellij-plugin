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

class KtlintAnnotator : ExternalAnnotator<LintResult, List<LintError>>() {
    override fun collectInformation(file: PsiFile): LintResult {
        val config = file.project.config()
        if (!config.enableKtlint || config.hideErrors) {
            return emptyLintResult()
        }

        return doLint(file, config, false)
    }

    override fun doAnnotate(collectedInfo: LintResult): List<LintError> {
        return collectedInfo.uncorrectedErrors
    }

    override fun apply(file: PsiFile, errors: List<LintError>, holder: AnnotationHolder) {
        val config = file.project.config()

        errors.forEach {
            val errorRange = errorTextRange(file, it)
            val message = "${it.detail} (${it.ruleId})"
            val severity = if (config.treatAsErrors) HighlightSeverity.ERROR else HighlightSeverity.WARNING

            holder.newAnnotation(severity, message).apply {
                range(errorRange)

                if (it.canBeAutoCorrected) withFix(FormatIntention())
                withFix(GlobalDisableRuleIntention(it.ruleId))
                withFix(LineDisableIntention(it))
                withFix(DisablePluginIntention())

                create()
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
