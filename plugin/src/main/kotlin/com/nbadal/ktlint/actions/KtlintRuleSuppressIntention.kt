package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintFeature.SHOW_INTENTION_TO_SUPPRESS_VIOLATION
import com.nbadal.ktlint.config
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.ktlintFormat
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtlintSuppressionAtOffset
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.api.insertSuppression

class KtlintRuleSuppressIntention(
    private val lintError: LintError,
) : BaseIntentionAction(),
    HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Suppress '${lintError.ruleId.value}'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        // Skip if no error element can be located for the error offset
        return psiFile.project.isEnabled(SHOW_INTENTION_TO_SUPPRESS_VIOLATION) && psiFile.findElementAtLintErrorOffset() != null
    }

    private fun PsiFile.findElementAtLintErrorOffset(): PsiElement? =
        viewProvider
            .document
            ?.takeIf { lintError.line <= it.lineCount }
            ?.let { doc ->
                findElementAt(doc.getLineStartOffset(lintError.line - 1) + lintError.col - 1)
            }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        psiFile
            .viewProvider
            .document
            ?.takeIf { lintError.line <= it.lineCount }
            ?.let { document ->
                // The psiFile may contain unsaved changes. So create a snippet based on content of the psiFile
                val code =
                    Code.fromSnippet(
                        // Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case
                        // the content of an active editor window is changed via a global find and replace, the document
                        // text is updated but the Psi (and PsiFile) have not yet been changed.
                        content = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.text,
                        script = psiFile.virtualFile.path.endsWith(".kts"),
                        // TODO: de-comment when parameter is supported in Ktlint 1.1.0
                        // path = psiFile.virtualFile.toNioPath(),
                    )
                project
                    .config()
                    .ktlintRuleEngine(null)
                    ?.insertSuppression(code, lintError.toKtlintSuppressionAtOffset())
                    ?.let { updatedCode ->
                        if (updatedCode != code.content) {
                            document.setText(updatedCode)
                            ktlintFormat(psiFile, "KtlintSuppressIntention")
                        }
                    }
            }
    }

    private fun LintError.toKtlintSuppressionAtOffset() =
        KtlintSuppressionAtOffset(
            line = line,
            col = col,
            ruleId = ruleId,
        )
}
