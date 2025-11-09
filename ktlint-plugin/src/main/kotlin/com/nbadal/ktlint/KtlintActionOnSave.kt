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
        documents: Array<Document>,
    ) {
        val psiFiles =
            with(FileDocumentManager.getInstance()) {
                documents
                    .mapNotNull { getFile(it) }
                    .mapNotNull { PsiManager.getInstance(project).findFile(it) }
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

                KtlintRuleEngineWrapper
                    .instance
                    .formatAllOpenFiles(
                        project = project,
                        ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                        triggeredBy = "KtlintActionOnSave",
                    )
            } else {
                // Only format files which were modified
                psiFiles.forEach { psiFile ->
                    KtlintRuleEngineWrapper
                        .instance
                        .format(
                            psiFile,
                            ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                            triggeredBy = "KtlintActionOnSave",
                        )
                }
            }
        }
    }
}

private val EDITOR_CONFIG_LANGUAGE = Language.findLanguageByID("EditorConfig")
