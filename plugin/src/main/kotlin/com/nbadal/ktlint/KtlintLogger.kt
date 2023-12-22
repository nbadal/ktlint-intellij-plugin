package com.nbadal.ktlint

import com.intellij.openapi.diagnostic.DefaultLogger

@Suppress("unused")
class KtlintLogger(
    qualifiedName: String?,
) : DefaultLogger(qualifiedName ?: "ktlint-intellij-plugin") {
    fun debug(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        logToStdOut(message = message(), throwable = throwable) ?: super.debug(message(), throwable)
    }

    fun info(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        logToStdOut(message = message(), throwable = throwable) ?: super.info(message(), throwable)
    }

    fun warn(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        logToStdOut(message = message(), throwable = throwable) ?: super.warn(message(), throwable)
    }

    fun error(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        logToStdOut(message = message(), throwable = throwable) ?: super.error(message(), throwable)
    }

    private fun logToStdOut(
        message: String? = null,
        throwable: Throwable? = null,
    ) = if (System.getenv(KTLINT_PLUGIN_LOG_TO_STDOUT).equals("true", ignoreCase = true)) {
        message?.let { println(message) }
        throwable?.let { println(throwable) }
    } else {
        null
    }

    companion object {
        const val KTLINT_PLUGIN_LOG_TO_STDOUT = "KTLINT_PLUGIN_LOG_TO_STDOUT"
    }
}
