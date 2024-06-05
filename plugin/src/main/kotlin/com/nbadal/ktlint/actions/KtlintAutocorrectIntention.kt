package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintViolationAutocorrectHandler
import com.nbadal.ktlint.config
import com.nbadal.ktlint.findElementAt
import com.nbadal.ktlint.ktlintFormat
import com.pinterest.ktlint.rule.engine.api.LintError

class KtlintAutocorrectIntention(
    private val lintError: LintError,
) : BaseIntentionAction(),
    HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Ktlint fix '${lintError.detail} (${lintError.ruleId.value})'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        if (lintError.ruleId !in project.config().ruleIdsWithAutocorrectApproveHandler) {
            // This rule does not implement the AutocorrectApproveHandler. As of that it is not able to autocorrect only this specific lint
            // error in case the PsiFile contains multiple errors which are emitted by the same rule.
            return false
        }

        // Skip if no error element can be located for the error offset
        return psiFile.findElementAt(lintError) != null
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        ktlintFormat(
            psiFile,
            KtlintViolationAutocorrectHandler(lintError),
            "KtlintAutocorrectIntention",
            forceFormat = true,
        )
    }
}
