package com.nbadal.ktlint.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.nbadal.ktlint.KtlintFeature.SHOW_MENU_OPTION_FORMAT_WITH_KTLINT
import com.nbadal.ktlint.KtlintFormatService
import com.nbadal.ktlint.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintNotifier.KtlintNotificationGroup.DEFAULT
import com.nbadal.ktlint.KtlintNotifier.notifyInformation
import com.nbadal.ktlint.KtlintNotifier.notifyWarning
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.FILE_RELATED_ERROR
import com.nbadal.ktlint.KtlintRuleEngineWrapper.KtlintResult.Status.SUCCESS
import com.nbadal.ktlint.isEnabled
import com.nbadal.ktlint.isKotlinFile
import com.nbadal.ktlint.ktlintMode

class FormatAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        event.apply {
            presentation.isEnabledAndVisible = showMenuOptionFormatWithKtlint() && isDirectoryOrKotlinFileSelected()
        }
    }

    private fun AnActionEvent.showMenuOptionFormatWithKtlint() =
        getData(CommonDataKeys.PROJECT)
            ?.run { isEnabled(SHOW_MENU_OPTION_FORMAT_WITH_KTLINT) }
            ?: false

    private fun AnActionEvent.isDirectoryOrKotlinFileSelected() =
        getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.any { it.isDirectory || it.isKotlinFile() }
            ?: false

    override fun actionPerformed(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = event.getData(CommonDataKeys.PROJECT) ?: return

        project
            .service<KtlintFormatService>()
            .formatFilesInBackground(files.toList(), triggeredBy = "FormatAction") { ktlintResults ->
                notifyFormatCompleted(project, ktlintResults)
            }
    }

    private fun notifyFormatCompleted(
        project: Project,
        ktlintResults: List<KtlintResult>,
    ) {
        val filesNotProcessedDueToError = ktlintResults.count { it.status == FILE_RELATED_ERROR }
        val filesChangedByFormat = ktlintResults.count { it.fileChangedByFormat }
        val filesFormatted = ktlintResults.count { it.status == SUCCESS }
        val message =
            listOfNotNull(
                "Formatting is completed.",
                "$filesNotProcessedDueToError files have not been formatted due to an error."
                    .takeIf { filesNotProcessedDueToError > 0 },
                "$filesChangedByFormat files have been formatted."
                    .takeIf { filesChangedByFormat > 0 },
                "Files might still contain ktlint violations which can not be autocorrected."
                    .takeIf { filesFormatted > 0 },
                "Get more value out of ktlint by enabling automatic formatting by using the 'distract free' mode."
                    .takeUnless { project.ktlintMode() == DISTRACT_FREE },
            ).joinToString(separator = " ")
        if (filesNotProcessedDueToError > 0) {
            notifyWarning(
                notificationGroup = DEFAULT,
                project = project,
                title = "Format with Ktlint",
                message = message,
            )
        } else {
            notifyInformation(
                notificationGroup = DEFAULT,
                project = project,
                title = "Format with Ktlint",
                message = message,
            )
        }
    }
}
