package com.nbadal.ktlint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object KtlintNotifier {
    fun notifyErrorWithSettings(project: Project, subtitle: String, content: String) =
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ktlint Notifications")
            .createNotification("ktlint Error", NotificationType.ERROR).apply {
                setSubtitle(subtitle)
                setContent(content)
                addAction(OpenSettingsAction(project))
                notify(project)
            }

    private class OpenSettingsAction(val project: Project) : NotificationAction("Open ktlint settings...") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
        }
    }
}
