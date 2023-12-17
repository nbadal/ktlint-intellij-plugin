package com.nbadal.ktlint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.util.applyIf

object KtlintNotifier {
    private const val KTLINT_NOTIFICATION_GROUP = "Ktlint Notifications"

    fun notifyError(
        project: Project,
        title: String,
        message: String,
        forceSettingsDialog: Boolean = false,
    ) = notify(project, title, message, ERROR, forceSettingsDialog)

    fun notifyWarning(
        project: Project,
        title: String,
        message: String,
        forceSettingsDialog: Boolean = false,
    ) = notify(project, title, message, WARNING, forceSettingsDialog)

    fun notifyInformation(
        project: Project,
        title: String,
        message: String,
        forceSettingsDialog: Boolean = false,
    ) = notify(project, title, message, INFORMATION, forceSettingsDialog)

    private fun notify(
        project: Project,
        title: String,
        message: String,
        notificationType: NotificationType,
        forceSettingsDialog: Boolean = false,
    ) = NotificationGroupManager
        .getInstance()
        .getNotificationGroup(KTLINT_NOTIFICATION_GROUP)
        .createNotification(title, message, notificationType)
        .applyIf(forceSettingsDialog || project.isEnabled(KtlintFeature.SHOW_INTENTION_SETTINGS_DIALOG)) {
            addAction(OpenSettingsAction(project))
        }.notify(project)

    private class OpenSettingsAction(
        val project: Project,
    ) : NotificationAction("Open ktlint settings...") {
        override fun actionPerformed(
            e: AnActionEvent,
            notification: Notification,
        ) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
        }
    }
}
