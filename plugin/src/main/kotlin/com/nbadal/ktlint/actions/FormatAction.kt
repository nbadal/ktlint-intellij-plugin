package com.nbadal.ktlint.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.config
import com.nbadal.ktlint.doLint

class FormatAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val file: VirtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        event.presentation.apply {
            isEnabledAndVisible = file.extension in setOf("kt", "kts") && project.config().enableKtlint
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = event.getData(CommonDataKeys.PROJECT)

        if (virtualFile != null && project != null) {
            PsiManager.getInstance(project).findFile(virtualFile)?.let {
                doLint(it, project.config(), true)
            }
        }
    }
}
