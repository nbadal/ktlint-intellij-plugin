package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.nbadal.ktlint.KtlintFeature.DISPLAY_ALL_VIOLATIONS
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.isKotlinFile
import com.nbadal.ktlint.setDisplayAllKtlintViolations

class LintAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.presentation.apply {
            isEnabledAndVisible = event.showMenuOptionLintWithKtlint() && event.isKotlinFile()
        }
    }

    private fun AnActionEvent.showMenuOptionLintWithKtlint() =
        getData(CommonDataKeys.PROJECT)
            ?.run { !isEnabled(DISPLAY_ALL_VIOLATIONS) }
            ?: false

    private fun AnActionEvent.isKotlinFile(): Boolean =
        getData(CommonDataKeys.VIRTUAL_FILE)
            ?.isKotlinFile()
            ?: false

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        editor.document.setDisplayAllKtlintViolations()
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
    }
}
