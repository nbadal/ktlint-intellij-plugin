package com.nbadal.ktlint

import com.intellij.ide.actionsOnSave.impl.ActionsOnSaveFileDocumentManagerListener.DocumentUpdatingActionOnSave
import com.intellij.lang.Language
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.KtlintFeature.FORMAT_WITH_KTLINT_ON_SAVE

/**
 * The platform calls [updateDocument] on a background thread for one document at a time, cancels it when the document is edited by the
 * user, and saves the document afterwards.
 */
class KtlintActionOnSave : DocumentUpdatingActionOnSave() {
    override val presentableName = "Ktlint"

    override fun isEnabledForProject(project: Project): Boolean =
        // As ktlint is configured by the '.editorconfig', it needs to respond on any save on that file for any mode in which ktlint is
        // actually executed. ActionOnSave for '.editorconfig' can not be disabled using configuration.
        project.ktlintMode() != KtlintMode.DISABLED

    override suspend fun updateDocument(
        project: Project,
        document: Document,
    ) {
        if (!project.isEnabled(FORMAT_WITH_KTLINT_ON_SAVE) || !project.config().formatOnSave) {
            return
        }

        val psiFile =
            readAction {
                FileDocumentManager
                    .getInstance()
                    .getFile(document)
                    ?.let { PsiManager.getInstance(project).findFile(it) }
            } ?: return

        if (psiFile.language == EDITOR_CONFIG_LANGUAGE) {
            // Save the ".editorconfig" file before formatting the open files so that the change is taken into account.
            edtWriteAction { FileDocumentManager.getInstance().saveDocument(document) }
            project
                .service<KtlintFormatService>()
                .formatAllOpenFilesInBackground(triggeredBy = "KtlintActionOnSave")
        } else {
            // Only format kotlin files which were modified
            project
                .service<KtlintFormatService>()
                .format(
                    psiFile,
                    ktlintFormatAutoCorrectHandler = KtlintFileAutocorrectHandler,
                    triggeredBy = "KtlintActionOnSave",
                    saveDocument = false,
                )
        }
    }
}

private val EDITOR_CONFIG_LANGUAGE = Language.findLanguageByID("EditorConfig")
