package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintConfigStorage

class KtlintGlobalDisableRuleAction(private val ruleId: String) : BaseIntentionAction() {
    override fun getFamilyName() = "ktlint"

    override fun getText() = "Disable '$ruleId' for project"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val config = KtlintConfigStorage.instance(project)

        val disabled = config.disabledRules.toMutableList()
        disabled.add(ruleId)
        config.disabledRules = disabled

        // Reanalyze:
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
