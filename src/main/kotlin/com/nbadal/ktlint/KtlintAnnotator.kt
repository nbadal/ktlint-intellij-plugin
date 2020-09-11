package com.nbadal.ktlint

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.LintError

class KtlintAnnotator : ExternalAnnotator<PsiFile, List<LintError>>() {
    override fun collectInformation(file: PsiFile) = file

    override fun doAnnotate(file: PsiFile): List<LintError>? {
        val config = KtlintConfigStorage.instance(file.project)
        if (!config.enableKtlint) {
            return emptyList()
        }

        return doLint(file, config.androidMode, config.useExperimental)
    }

    override fun apply(file: PsiFile, errors: List<LintError>, holder: AnnotationHolder) {
        val doc = PsiDocumentManager.getInstance(file.project).getDocument(file)!!
        val config = KtlintConfigStorage.instance(file.project)

        errors.forEach {
            // We only get line + col currently, so mark the whole line.
            val errorRange = TextRange(doc.getLineStartOffset(it.line - 1), doc.getLineEndOffset(it.line - 1))
            val message = "${it.detail} (${it.ruleId})"

            if (config.treatAsErrors) {
                holder.createErrorAnnotation(errorRange, message)
            } else {
                holder.createWarningAnnotation(errorRange, message)
            }
        }
    }
}
