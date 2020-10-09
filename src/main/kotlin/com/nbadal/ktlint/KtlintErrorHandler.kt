package com.nbadal.ktlint

import com.intellij.diagnostic.IdeaReportingEvent
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
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        val config = withAccessToken(BuildConfig.ROLLBAR_ACCESS_TOKEN).apply {
            environment("production")
            appPackages(listOf(BuildConfig.NAME))
            codeVersion(BuildConfig.VERSION)
        }.build()

        val rollbar = Rollbar.init(config)
        events.forEach { event ->
            when (event) {
                is IdeaReportingEvent -> {
                    rollbar.error(event.data.throwable)
                }
                else -> {
                    rollbar.error(event.throwable, event.message)
                }
            }
        }

        rollbar.close(true)

        val status = SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE)
        consumer.consume(status)
        return true
    }
}
