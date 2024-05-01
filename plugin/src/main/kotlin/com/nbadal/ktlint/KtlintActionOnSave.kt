package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.ActionOnSave
import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE

class KtlintActionOnSave : ActionOnSave() {
    override fun isEnabledForProject(project: Project): Boolean =
        // As ktlint is configured by the '.editorconfig', it needs to respond on any save on that file for any mode in which ktlint is
        // actually executed. ActionOnSave for '.editorconfig' can not be disabled using configuration.
        project.ktlintMode() != KtlintMode.DISABLED

    override fun processDocuments(
        project: Project,
        documents: Array<out Document>,
    ) {
        val psiFiles =
            with(FileDocumentManager.getInstance()) {
                documents
                    .mapNotNull { getFile(it) }
                    .mapNotNull { PsiManager.getInstance(project).findFile(it) }
            }

        // If ktlint is not disabled, then always respond on saving the '.editorconfig' to ensure that KtlintAnnotator will pick up changes
        // in violations due to change settings.
        if (psiFiles.any { it.language == EDITOR_CONFIG_LANGUAGE }) {
            project.resetKtlintAnnotator()
        }

        if (project.isEnabled(FORMAT_WITH_KTLINT_ON_SAVE) && project.config().formatOnSave) {
            if (psiFiles.any { it.language == EDITOR_CONFIG_LANGUAGE }) {
                // Save all ".editorconfig" files before processing other changed documents so the changed ".editorconfig" files are taken
                // into account while processing those documents.
                psiFiles
                    .filter { it.language == EDITOR_CONFIG_LANGUAGE }
                    .forEach {
                        FileDocumentManager.getInstance().saveDocument(it.viewProvider.document)
                    }

                // Format all files in open editors
                FileEditorManager
                    .getInstance(project)
                    .openFiles
                    .forEach { virtualFile ->
                        PsiManager
                            .getInstance(project)
                            .findFile(virtualFile)
                            ?.let { psiFile ->
                                ktlintFormat(psiFile, ktlintFormatRange = KtlintFileFormatRange, triggeredBy = "KtlintActionOnSave")
                            }
                    }
            } else {
                // Only format files which were modified
                psiFiles.forEach { psiFile ->
                    ktlintFormat(psiFile, ktlintFormatRange = KtlintFileFormatRange, triggeredBy = "KtlintActionOnSave")
                }
            }
        }
    }
}

private val EDITOR_CONFIG_LANGUAGE = Language.findLanguageByID("EditorConfig")
