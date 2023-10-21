package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class KtlintActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean {
        return project.config().formatOnSave
    }

    override fun processDocuments(project: Project, documents: Array<Document?>) {
        if (!project.config().enableKtlint || !project.config().formatOnSave) return

        val manager = FileDocumentManager.getInstance()
        documents
            .filterNotNull()
            .mapNotNull { manager.getFile(it)?.toPsiFile(project) }
            .forEach { psiFile -> ktlintFormat(psiFile, "KtlintActionOnSave") }
    }
}
