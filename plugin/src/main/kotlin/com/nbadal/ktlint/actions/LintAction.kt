package com.nbadal.ktlint.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.nbadal.ktlint.displayAllKtlintViolations
import com.nbadal.ktlint.isKotlinFile

class LintAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        event.presentation.apply {
            isEnabledAndVisible = file.isKotlinFile()
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        println("Start lintAction.actionPerformed")
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        println("editor: $editor")

//        FileEditorManager
//            .getInstance(project)
//            .getSelectedTextEditor()
//            .let { x -> }
        println("Before displayAllKtlintViolations")
        editor.displayAllKtlintViolations(true)
        println("After displayAllKtlintViolations")
        DaemonCodeAnalyzer.getInstance(project).restart()
    }
}
