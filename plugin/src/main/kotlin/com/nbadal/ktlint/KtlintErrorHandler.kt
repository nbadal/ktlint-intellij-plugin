package com.nbadal.ktlint

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder.withAccessToken
import java.awt.Component

class KtlintErrorHandler : ErrorReportSubmitter() {
    override fun getReportActionText(): String = "Report"

    override fun submit(
        events: Array<out IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<in SubmittedReportInfo>,
    ): Boolean {
        val config = withAccessToken(BuildConfig.ROLLBAR_ACCESS_TOKEN).apply {
            environment("production")
            appPackages(listOf(BuildConfig.NAME))
            codeVersion(BuildConfig.VERSION)
        }.build()

        val rollbar = Rollbar.init(config)
        events.forEach { event ->
            val extras = mapOf(
                "last_action" to IdeaLogger.ourLastActionId,
                "additional_info" to additionalInfo,
                "ide_build" to ApplicationInfo.getInstance().build.asString(),
            ).filterValues { it != null }

            when (event) {
                is IdeaReportingEvent -> {
                    rollbar.error(event.data.throwable, extras)
                }
                else -> {
                    rollbar.error(event.throwable, extras, event.message)
                }
            }
        }

        rollbar.close(true)

        val status = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
        consumer.consume(status)
        return true
    }
}
