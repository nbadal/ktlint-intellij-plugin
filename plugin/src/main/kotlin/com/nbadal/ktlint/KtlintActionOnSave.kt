package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class KtlintActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean {
        return project.ktlintEnabled()
    }

    override fun processDocuments(
        project: Project,
        documents: Array<out Document>,
    ) {
        with(FileDocumentManager.getInstance()) {
            documents
                .mapNotNull { getFile(it) }
                .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                .forEach { psiFile -> ktlintFormat(psiFile, "KtlintActionOnSave") }
        }
    }
}
