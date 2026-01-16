package com.nbadal.ktlint.plugin

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class KtlintOpenSettingsDoNotShowAgainIntention :
    BaseIntentionAction(),
    LowPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Don't show again"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = project.isEnabled(KtlintFeature.SHOW_INTENTION_SETTINGS_DIALOG)

    /**
     * As [isAvailable] return true always, the [invoke] is also called when previewing the result of the intention unless this function
     * returns null. This prevents that the plugin is either enabled/disabled unintentionally.
     */
    override fun getFileModifierForPreview(target: PsiFile): FileModifier? = null

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        KtlintApplicationSettings.getInstance().state.showBanner = false
        // Re-inspect to get the banner removed
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    override fun startInWriteAction() = false
}
