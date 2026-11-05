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
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.NOT_STARTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /**
     * Ktlint blocks its thread, and at most one file per project is processed at a time. A [Mutex] is used instead of
     * `Dispatchers.IO.limitedParallelism(1)`, as the signature of the latter differs between the coroutines libraries which are bundled
     * with the supported IDE versions.
     */
    private val ktlintMutex = Mutex()

    fun formatInBackground(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
    ) {
        launchWithProgress { format(psiFile, ktlintFormatAutoCorrectHandler, triggeredBy, forceFormat) }
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
        onCompleted: (List<KtlintResult>) -> Unit,
    ) {
        launchWithProgress {
            onCompleted(formatFiles(kotlinFilesIn(roots), triggeredBy, forceFormat = true))
        }
    }

    /**
     * Formats the file in the calling coroutine. When called from an action on save, the [saveDocument] should be false as the platform
     * saves the document afterwards.
     */
    suspend fun format(
        psiFile: PsiFile,
        ktlintFormatAutoCorrectHandler: KtlintFormatAutocorrectHandler,
        triggeredBy: String,
        forceFormat: Boolean = false,
        saveDocument: Boolean = true,
    ): KtlintResult {
        val ktlintRuleEngineWrapper = KtlintRuleEngineWrapper.instance
        if (!ktlintRuleEngineWrapper.isFormatAllowed(psiFile, forceFormat)) {
            return KtlintResult(NOT_STARTED)
        }

        val snapshot = readAction { KtlintFileSnapshot.from(psiFile) } ?: return KtlintResult(NOT_STARTED)
        val ktlintFormatOutput =
            ktlintMutex.withLock {
                withContext(Dispatchers.IO) {
                    ktlintRuleEngineWrapper.format(snapshot, ktlintFormatAutoCorrectHandler, triggeredBy)
                }
            }
        val formattedCode = ktlintFormatOutput.formattedCode
        if (formattedCode != null && !applyToDocument(snapshot, formattedCode)) {
            logger.debug { "Discard ktlint format result of '${psiFile.virtualFile.name}' as the document has been changed" }
            return KtlintResult(NOT_STARTED)
        }

        if (saveDocument) {
            edtWriteAction { FileDocumentManager.getInstance().saveDocument(snapshot.document) }
        }
        return ktlintFormatOutput.appliedKtlintResult()
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
    ): List<KtlintResult> =
        reportSequentialProgress(files.size) { reporter ->
            files.map { file ->
                reporter.itemStep(file.name) {
                    readAction { file.takeIf { it.isValid }?.let { PsiManager.getInstance(project).findFile(it) } }
                        ?.let { psiFile -> format(psiFile, KtlintFileAutocorrectHandler, triggeredBy, forceFormat) }
                        ?: KtlintResult(NOT_STARTED)
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
    private suspend fun applyToDocument(
        snapshot: KtlintFileSnapshot,
        formattedCode: String,
    ): Boolean =
        writeCommandAction(project, "Format with Ktlint") {
            if (snapshot.isStale) {
                false
            } else {
                PsiDocumentManager
                    .getInstance(project)
                    .doPostponedOperationsAndUnblockDocument(snapshot.document)
                snapshot.document.setText(formattedCode)
                true
            }
        }
}
