package com.nbadal.ktlint.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.nbadal.ktlint.KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT
import com.nbadal.ktlint.KtlintNotifier.notifyInformation
import com.nbadal.ktlint.KtlintNotifier.notifyWarning
import com.nbadal.ktlint.KtlintResult
import com.nbadal.ktlint.actions.FormatAction.KtlintFormatContentIterator.BatchStatus.FILE_RELATED_ERROR
import com.nbadal.ktlint.actions.FormatAction.KtlintFormatContentIterator.BatchStatus.PLUGIN_CONFIGURATION_ERROR
import com.nbadal.ktlint.actions.FormatAction.KtlintFormatContentIterator.BatchStatus.SUCCESS
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.ktlintEnabled
import com.nbadal.ktlint.ktlintFormat

class FormatAction : AnAction() {
    override fun update(event: AnActionEvent) {
        val project = event.getData(CommonDataKeys.PROJECT) ?: return
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        event.presentation.apply {
            isEnabledAndVisible = project.isEnabled(SHOW_MENU_OPTION_FORMAT_WITH_KTLINT) && files.isNotEmpty()
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        val ktlintFormatContentIterator = KtlintFormatContentIterator(project)
        files.forEach {
            VfsUtilCore.iterateChildrenRecursively(it, null, ktlintFormatContentIterator)
        }

        val message =
            listOfNotNull(
                "Formatting is completed.",
                "${ktlintFormatContentIterator.filesNotProcessedDueToError} files have not been formatted due to an error."
                    .takeIf { ktlintFormatContentIterator.filesNotProcessedDueToError > 0 },
                "${ktlintFormatContentIterator.filesChangedByFormat} files have been formatted."
                    .takeIf { ktlintFormatContentIterator.filesChangedByFormat > 0 },
                "Files might still contain ktlint violations which can not be autocorrected."
                    .takeIf { ktlintFormatContentIterator.filesFormatted > 0 },
                "Get more value out of ktlint by enabling automatic formatting.".takeUnless { project.ktlintEnabled() },
            ).joinToString(separator = " ")
        when (ktlintFormatContentIterator.status) {
            SUCCESS -> {
                notifyInformation(
                    project = project,
                    title = "Format with Ktlint",
                    message = message,
                )
            }

            FILE_RELATED_ERROR -> {
                notifyWarning(
                    project = project,
                    title = "Format with Ktlint",
                    message = message,
                )
            }

            PLUGIN_CONFIGURATION_ERROR -> {
                // Notification is already sent by plugin
            }
        }
    }

    private class KtlintFormatContentIterator(
        val project: Project,
    ) : ContentIterator {
        var status = SUCCESS
        var filesChangedByFormat = 0
        var filesFormatted = 0
        var filesNotProcessedDueToError = 0

        override fun processFile(fileOrDir: VirtualFile): Boolean {
            if (fileOrDir.isDirectory) {
                return true
            }

            val ktlintResult =
                PsiManager
                    .getInstance(project)
                    .findFile(fileOrDir)
                    ?.let { ktlintFormat(it, "FormatAction", forceFormat = true) }
            // In case an error occurs, a notification has already been sent by ktlintFormat above
            when (ktlintResult?.status) {
                KtlintResult.Status.SUCCESS -> {
                    // Status of iterator is not changed as it should only be SUCCESS when all files have been processed successful
                    if (ktlintResult.fileChangedByFormat) {
                        filesChangedByFormat += 1
                    }
                    filesFormatted += 1
                    return true
                }

                KtlintResult.Status.PLUGIN_CONFIGURATION_ERROR -> {
                    status = PLUGIN_CONFIGURATION_ERROR
                    // As the same error will occur for every file, stop processing
                    return false
                }

                null, KtlintResult.Status.NOT_STARTED -> {
                    // File is not a kotlin file
                    return true
                }

                KtlintResult.Status.FILE_RELATED_ERROR -> {
                    status = FILE_RELATED_ERROR
                    filesNotProcessedDueToError += 1
                    return true
                }
            }
        }

        enum class BatchStatus { SUCCESS, PLUGIN_CONFIGURATION_ERROR, FILE_RELATED_ERROR }
    }
}
