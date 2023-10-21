package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class KtlintActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean {
        return project.config().enableKtlint
    }

    override fun processDocuments(project: Project, documents: Array<out Document>) {
        with(FileDocumentManager.getInstance()) {
            documents
                .mapNotNull { getFile(it)?.toPsiFile(project) }
                .forEach { psiFile -> ktlintFormat(psiFile, "KtlintActionOnSave") }
        }
    }
}
