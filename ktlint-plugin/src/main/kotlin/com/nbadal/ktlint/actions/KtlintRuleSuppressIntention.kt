package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintFeature.SHOW_INTENTION_TO_SUPPRESS_VIOLATION
import com.nbadal.ktlint.KtlintFileAutocorrectHandler
import com.nbadal.ktlint.KtlintRuleEngineWrapper
import com.nbadal.ktlint.config
import com.nbadal.ktlint.findElementAt
import com.nbadal.ktlint.isEnabled
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtlintSuppressionAtOffset
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.api.insertSuppression

class KtlintRuleSuppressIntention(
    private val lintError: LintError,
) : BaseIntentionAction(),
    HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Ktlint suppress '${lintError.ruleId.value}'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        // Skip if no error element can be located for the error offset
        return psiFile.project.isEnabled(SHOW_INTENTION_TO_SUPPRESS_VIOLATION) && psiFile.findElementAt(lintError) != null
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
                    Code.fromSnippetWithPath(
                        // Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case the content of an
                        // active editor window is changed via a global find and replace, the document text is updated but the Psi (and
                        // PsiFile) have not yet been changed.
                        content = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.text,
                        virtualPath = psiFile.virtualFile.toNioPath(),
                    )
                project
                    .config()
                    .ktlintRuleEngine
                    ?.insertSuppression(code, lintError.toKtlintSuppressionAtOffset())
                    ?.let { updatedCode ->
                        if (updatedCode != code.content) {
                            document.setText(updatedCode)
                            KtlintRuleEngineWrapper
                                .instance
                                .format(
                                    psiFile,
                                    ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                                    triggeredBy = "KtlintSuppressIntention",
                                )
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
