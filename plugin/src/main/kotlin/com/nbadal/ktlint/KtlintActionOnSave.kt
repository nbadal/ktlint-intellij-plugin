package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager

class KtlintActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean = project.ktlintEnabled()

    override fun processDocuments(
        project: Project,
        documents: Array<out Document>,
    ) {
        if (project.config().formatOnSave) {
            val psiFiles =
                with(FileDocumentManager.getInstance()) {
                    documents
                        .mapNotNull { getFile(it) }
                        .mapNotNull { PsiManager.getInstance(project).findFile(it) }
                }

            // If an any ".editorconfig" file is saved, then complete saving of that file and reset the KtlintRuleEngine before processing the
            // other changed documents so the change ".editorconfig" is taken into account while processing those documents
            psiFiles
                .firstOrNull { it.language == EDITOR_CONFIG_LANGUAGE }
                ?.let {
                    // Complete the saving of the ".editorconfig" before processing
                    FileDocumentManager.getInstance().saveDocument(it.viewProvider.document)
                    project.config().resetKtlintRuleEngine()
                }

            psiFiles.forEach { psiFile -> ktlintFormat(psiFile, "KtlintActionOnSave") }
        }
    }
}

private val EDITOR_CONFIG_LANGUAGE = Language.findLanguageByID("EditorConfig")
