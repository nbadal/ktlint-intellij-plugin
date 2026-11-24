package com.nbadal.ktlint

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.KtlintExecutionResult.FormatResult
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger = KtlintLogger()

/**
 * Runs ktlint format in coroutines which are canceled when the project is closed or the plugin is unloaded. Ktlint itself can not be
 * canceled. Cancellation takes effect when ktlint has finished processing the current file.
 */
@Service(Service.Level.PROJECT)
internal class KtlintFormatService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
) {
    fun formatInBackground(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ) {
        launchWithProgress { formatAndSaveDocument(psiFile, ktlintFormatAutoCorrectHandler, triggeredBy, forceFormat) }
    }

    fun formatAllOpenFilesInBackground(triggeredBy: String) {
        launchWithProgress {
            val openKotlinFiles =
                FileEditorManager
                    .getInstance(project)
                    .openFiles
                    .filter { it.isKotlinFile() }
            formatFiles(openKotlinFiles, triggeredBy, forceFormat = false)
        }
    }

    /**
     * Formats the kotlin files in [roots], including their subdirectories. The [onCompleted] is not called when the user cancels.
     */
    fun formatFilesInBackground(
        roots: List<VirtualFile>,
        triggeredBy: String,
        onCompleted: (List<KtlintExecutionResult>) -> Unit,
    ) {
        launchWithProgress {
            onCompleted(formatFiles(kotlinFilesIn(roots), triggeredBy, forceFormat = true))
        }
    }

    /**
     * Formats the file and saves the document in the calling coroutine.
     */
    private suspend fun formatAndSaveDocument(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ): KtlintExecutionResult =
        ktlintDocumentSnapshot(psiFile, forceFormat)
            ?.let { ktlintDocumentSnapshot ->
                format(ktlintDocumentSnapshot, ktlintFormatAutoCorrectHandler, triggeredBy)
                    .also {
                        if (it is FormatResult && it.contentChanged) {
                            edtWriteAction { FileDocumentManager.getInstance().saveDocument(ktlintDocumentSnapshot.document) }
                        }
                    }
            }
            ?: KtlintExecutionResult.NotExecuted

    /**
     * Formats the file in the calling coroutine. The file is not saved as the platform saves the document afterward.
     */
    suspend fun formatOnSave(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ): KtlintExecutionResult =
        ktlintDocumentSnapshot(psiFile, forceFormat)
            ?.let { ktlintDocumentSnapshot -> format(ktlintDocumentSnapshot, ktlintFormatAutoCorrectHandler, triggeredBy) }
            ?: KtlintExecutionResult.NotExecuted

    private suspend fun ktlintDocumentSnapshot(
        psiFile: PsiFile,
        forceFormat: Boolean = false,
    ): KtlintDocumentSnapshot? =
        psiFile
            .takeIf { it.virtualFile.isKotlinFile() }
            ?.takeIf { KtlintRuleEngineWrapper.instance.isFormatAllowed(it, forceFormat) }
            ?.let { readAction { KtlintDocumentSnapshot.from(it) } }

    private suspend fun format(
        ktlintDocumentSnapshot: KtlintDocumentSnapshot,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
    ): KtlintExecutionResult {
        val ktlintFormatServiceResult =
            // Ktlint blocks its thread while processing the given file. So limit parallel processing to a single thread.
            withContext(Dispatchers.IO.limitedParallelism(1)) {
                KtlintRuleEngineWrapper.instance.format(ktlintDocumentSnapshot, ktlintFormatAutoCorrectHandler, triggeredBy)
            }
        return ktlintFormatServiceResult
            .safeAs<FormatResult>()
            ?.let { formatResult ->
                if (formatResult.contentChanged && !ktlintDocumentSnapshot.applyToDocument(formatResult.formattedCode)) {
                    KtlintExecutionResult.NotExecuted
                } else {
                    formatResult
                }
            }
            ?: ktlintFormatServiceResult
    }

    private fun launchWithProgress(action: suspend () -> Unit) {
        coroutineScope.launch(ModalityState.defaultModalityState().asContextElement()) {
            withBackgroundProgress(project, "Format with Ktlint") { action() }
        }
    }

    private suspend fun formatFiles(
        files: Collection<VirtualFile>,
        triggeredBy: String,
        forceFormat: Boolean,
    ): List<KtlintExecutionResult> =
        reportSequentialProgress(files.size) { reporter ->
            files.map { file ->
                reporter.itemStep(file.name) {
                    readAction { file.takeIf { it.isValid }?.let { PsiManager.getInstance(project).findFile(it) } }
                        ?.let { psiFile -> formatAndSaveDocument(psiFile, KtlintFileAutocorrectHandler, triggeredBy, forceFormat) }
                        ?: KtlintExecutionResult.NotExecuted
                }
            }
        }

    /**
     * Each file is returned once, also when the [roots] overlap. For example, when both a directory and a file inside that directory are
     * selected.
     */
    private fun kotlinFilesIn(roots: List<VirtualFile>): Set<VirtualFile> =
        buildSet {
            for (root in roots) {
                VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
                    if (!file.isDirectory && file.isKotlinFile()) {
                        this += file
                    }
                    true
                }
            }
        }

    /**
     * The document has to be changed via [writeCommandAction] from the calling coroutine. An action on save is otherwise canceled by the
     * platform as the change is regarded as an edit by the user.
     */
    private suspend fun KtlintDocumentSnapshot.applyToDocument(formattedCode: String): Boolean =
        @Suppress("UnstableApiUsage")
        writeCommandAction(project, "Format with Ktlint") {
            if (isStale) {
                logger.debug { "Discard ktlint format result of '${psiFile.virtualFile.name}' as the document has been changed" }
                false
            } else {
                PsiDocumentManager
                    .getInstance(project)
                    .doPostponedOperationsAndUnblockDocument(document)
                @Suppress("UsePropertyAccessSyntax")
                document.setText(formattedCode)
                true
            }
        }
}
