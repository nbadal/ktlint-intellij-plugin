package com.nbadal.ktlint.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.nbadal.ktlint.config
import com.nbadal.ktlint.ktlintFormat
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class FormatAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        event.presentation.apply {
            isEnabledAndVisible = files.isNotEmpty() && project.config().enableKtlint
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        val ktlintFormatContentIterator = KtlintFormatContentIterator(project)
        files.forEach {
            VfsUtilCore.iterateChildrenRecursively(it, null, ktlintFormatContentIterator)
        }
    }

    class KtlintFormatContentIterator(val project: Project) : ContentIterator {
        override fun processFile(fileOrDir: VirtualFile): Boolean {
            fileOrDir
                .takeIf { it.isFile }
                ?.toPsiFile(project)
                ?.let { ktlintFormat(it, "FormatAction") }
            return true
        }
    }
}
