package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISABLED
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.MANUAL
import com.nbadal.ktlint.config
import com.nbadal.ktlint.ktlintMode

class KtlintModeIntention(
    private val ktlintMode: KtlintConfigStorage.KtlintMode,
) : BaseIntentionAction(),
    LowPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() =
        when (ktlintMode) {
            DISTRACT_FREE -> "Enable ktlint distract free mode for project"
            MANUAL -> "Enable ktlint manual mode for project"
            DISABLED -> "Disable ktlint for project"
            else -> throw UnsupportedOperationException("KtlintMode $ktlintMode can not be set via KtlintModeIntention")
        }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = project.ktlintMode() != ktlintMode

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
        project.config().ktlintMode = ktlintMode
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
