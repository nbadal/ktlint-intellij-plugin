package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintFeature
import com.nbadal.ktlint.KtlintFileAutocorrectHandler
import com.nbadal.ktlint.KtlintRuleEngineWrapper
import com.nbadal.ktlint.isEnabled

/**
 * When Ktlint is not run automatically, it will add a file analysis problem when at least one ktlint violation is found in the file that
 * could have been autocorrected. This intention invokes ktlint format on the file, regardless whether the plugin is enabled/disabled.
 */
class KtlintFormatIntention :
    BaseIntentionAction(),
    LowPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Format file with Ktlint"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = project.isEnabled(KtlintFeature.SHOW_PROBLEM_WITH_NUMBER_OF_KTLINT_VIOLATIONS_THAT_CAN_BE_AUTOCORRECTED)

    /**
     * As [isAvailable] return true always, the [invoke] is also called when previewing the result of the intention unless this function
     * returns null. This prevents that the code if formatted unintentionally.
     */
    override fun getFileModifierForPreview(target: PsiFile): FileModifier? = null

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        KtlintRuleEngineWrapper
            .instance
            .format(
                psiFile,
                ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                triggeredBy = "KtlintFormatIntention",
                forceFormat = true,
            )
    }
}
