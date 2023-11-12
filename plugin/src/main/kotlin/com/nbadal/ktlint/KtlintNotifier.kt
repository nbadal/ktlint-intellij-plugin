package com.nbadal.ktlint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object KtlintNotifier {
    private const val KTLINT_NOTIFICATION_GROUP = "Ktlint Notifications"

    fun notifyWarning(
        project: Project,
        title: String,
        message: String,
    ) = createNotification(title, message, NotificationType.WARNING).notify(project)

    fun notifyError(
        project: Project,
        title: String,
        message: String,
    ) = createNotification(title, message, NotificationType.ERROR).notify(project)

    fun notifyErrorWithSettings(
        project: Project,
        title: String,
        message: String,
    ) = createNotification(title, message, NotificationType.ERROR)
        .addAction(OpenSettingsAction(project))
        .notify(project)

    private fun createNotification(
        title: String,
        message: String,
        notificationType: NotificationType,
    ) = NotificationGroupManager
        .getInstance()
        .getNotificationGroup(KTLINT_NOTIFICATION_GROUP)
        .createNotification(title, message, notificationType)

    private class OpenSettingsAction(val project: Project) : NotificationAction("Open ktlint settings...") {
        override fun actionPerformed(
            e: AnActionEvent,
            notification: Notification,
        ) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
        }
    }
}
