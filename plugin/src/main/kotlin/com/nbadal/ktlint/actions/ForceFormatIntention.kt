package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintFeature
import com.nbadal.ktlint.KtlintFileAutocorrectHandler
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.ktlintFormat

/**
 * Intention to format the code with ktlint, regardless whether the plugin is enabled/disabled.
 */
class ForceFormatIntention :
    BaseIntentionAction(),
    LowPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Format file with ktlint"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = project.isEnabled(KtlintFeature.SHOW_INTENTION_FORCE_FORMAT_WITH_KTLINT)

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
        ktlintFormat(
            psiFile,
            ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
            triggeredBy = "ForceFormatIntention",
            forceFormat = true,
        )
    }
}
