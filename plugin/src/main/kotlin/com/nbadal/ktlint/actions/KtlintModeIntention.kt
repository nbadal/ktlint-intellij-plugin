package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintConfigStorage
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.ENABLED
import com.nbadal.ktlint.config

class KtlintModeIntention(private val ktlintMode: KtlintConfigStorage.KtlintMode) : BaseIntentionAction() {
    override fun getFamilyName() = "KtLint"

    override fun getText() =
        if (ktlintMode == ENABLED) {
            "Enable ktlint for project"
        } else {
            "Disable ktlint for project"
        }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean = true

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        project.config().ktlintMode = ktlintMode
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
