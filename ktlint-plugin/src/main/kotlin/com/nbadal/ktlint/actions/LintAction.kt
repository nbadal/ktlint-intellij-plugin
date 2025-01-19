package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.nbadal.ktlint.isKotlinFile
import com.nbadal.ktlint.setDisplayAllKtlintViolations

class LintAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        event.presentation.apply {
            isEnabledAndVisible = virtualFile.isKotlinFile()
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return

        editor.document.setDisplayAllKtlintViolations()
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
