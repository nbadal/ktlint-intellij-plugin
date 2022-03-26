package com.nbadal.ktlint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object KtlintNotifier {
    private val NOTIFICATION_GROUP =
        NotificationGroup("ktlint Notifications", NotificationDisplayType.BALLOON, true)

    fun notifyErrorWithSettings(project: Project, subtitle: String, content: String) {
        NOTIFICATION_GROUP.createNotification("ktlint Error", subtitle, content, NotificationType.ERROR)
            .addAction(OpenSettingsAction(project))
            .notify(project)
    }

    private class OpenSettingsAction(val project: Project) : NotificationAction("Open ktlint settings...") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, KtlintConfig::class.java)
        }
    }
}
