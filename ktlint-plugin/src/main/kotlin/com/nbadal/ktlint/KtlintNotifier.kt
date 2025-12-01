package com.nbadal.ktlint

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project

private val logger = KtlintLogger(KtlintProjectSettings::class.qualifiedName)

object KtlintNotifier {
    // Notification groups should be used for related notifications. Note that a user is abled to disable notifications per group only.
    // Notification groups are defined in file "plugin.xml". The title associated with the enum value should match with the title in the file "plugin.xml"
    enum class KtlintNotificationGroup(
        val title: String,
    ) {
        // Configuration messages are shown as sticky balloons by default as they should be ignored only explicitly by user
        CONFIGURATION("Ktlint Configuration"),

        // Rule exception messages are shown as sticky balloons by default as the user should report problems with rules to the maintainer
        RULE("Ktlint Rule"),

        // Default message are shown as normal balloons which disappear automatically. These should be used for less important messages.
        DEFAULT("Ktlint Generic"),
    }

    fun notifyError(
        notificationGroup: KtlintNotificationGroup,
        project: Project,
        title: String,
        message: String,
        notificationCustomizer: Notification.() -> Notification = { this },
    ) = notify(notificationGroup, project, title, message, ERROR, notificationCustomizer)

    fun notifyWarning(
        notificationGroup: KtlintNotificationGroup,
        project: Project,
        title: String,
        message: String,
        notificationCustomizer: (Notification.() -> Notification) = { this },
    ) = notify(notificationGroup, project, title, message, WARNING, notificationCustomizer)

    fun notifyInformation(
        notificationGroup: KtlintNotificationGroup,
        project: Project,
        title: String,
        message: String,
        notificationCustomizer: (Notification.() -> Notification) = { this },
    ) = notify(notificationGroup, project, title, message, INFORMATION, notificationCustomizer)

    private fun notify(
        notificationGroup: KtlintNotificationGroup,
        project: Project,
        title: String,
        message: String,
        notificationType: NotificationType,
        notificationCustomizer: (Notification.() -> Notification),
    ) = NotificationGroupManager
        .getInstance()
        .getNotificationGroup(notificationGroup.title)
        .createNotification(title, message, notificationType)
        .apply { notificationCustomizer() }
        .notify(project)
        .also {
            when (notificationType) {
                ERROR -> logger.error { message }
                WARNING -> logger.warn { message }
                else -> logger.debug { message }
            }
        }
}
