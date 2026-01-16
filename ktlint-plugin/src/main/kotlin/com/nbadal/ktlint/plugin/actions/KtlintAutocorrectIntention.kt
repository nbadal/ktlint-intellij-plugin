package com.nbadal.ktlint.plugin.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.connector.LintError
import com.nbadal.ktlint.plugin.KtlintRuleEngineWrapper
import com.nbadal.ktlint.plugin.KtlintViolationAutocorrectHandler
import com.nbadal.ktlint.plugin.findElementAt

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
        if (lintError.ruleId !in KtlintRuleEngineWrapper.Companion.instance.ruleIdsWithAutocorrectApproveHandler(psiFile)) {
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
        KtlintRuleEngineWrapper.Companion
            .instance
            .format(
                psiFile,
                KtlintViolationAutocorrectHandler(lintError),
                "KtlintAutocorrectIntention",
                forceFormat = true,
            )
    }
}
