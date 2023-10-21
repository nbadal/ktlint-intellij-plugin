package com.nbadal.ktlint.intentions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.config

class DisablePluginIntention : BaseIntentionAction() {
    override fun getFamilyName() = "ktlint"

    override fun getText() = "Disable ktlint"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        // Disable and restart analyzer to clear annotations
        project.config().enableKtlint = false
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
