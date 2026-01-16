package com.nbadal.ktlint.plugin.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.plugin.KtlintFeature.SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.plugin.isEnabled
import com.nbadal.ktlint.plugin.setDisplayAllKtlintViolations

/**
 * Intention to format the code with ktlint, regardless whether the plugin is enabled/disabled.
 */
class ShowAllKtlintViolationsIntention :
    BaseIntentionAction(),
    LowPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Show all Ktlint violations in file"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = project.isEnabled(SHOW_INTENTION_TO_DISPLAY_ALL_VIOLATIONS)

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
        editor?.document?.setDisplayAllKtlintViolations()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
