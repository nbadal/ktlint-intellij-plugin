package com.nbadal.ktlint

import org.jetbrains.kotlin.utils.PrintingLogger

@Suppress("unused")
class KtlintLogger : PrintingLogger(System.out) {
    fun debug(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        super.debug("[DEBUG] ${message()}", throwable)
    }

    fun info(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        super.debug("[INFO ] ${message()}", throwable)
    }

    fun warn(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        super.debug("[WARN ] ${message()}", throwable)
    }

    fun error(
        throwable: Throwable? = null,
        message: () -> String?,
    ) {
        super.debug("[ERROR] ${message()}", throwable)
    }

    companion object {
        const val KTLINT_PLUGIN_LOG_TO_STDOUT = "KTLINT_PLUGIN_LOG_TO_STDOUT"
    }
}
