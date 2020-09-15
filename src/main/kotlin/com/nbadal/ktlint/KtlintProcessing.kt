package com.nbadal.ktlint

import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.experimental.ExperimentalRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider

internal fun doLint(
    file: PsiFile,
    config: KtlintConfigStorage,
    format: Boolean
): LintResult {
    val disabledRules: List<String>? = null
    val editorConfigPath: String? = null

    val userData = listOfNotNull(
        "android" to config.androidMode.toString(),
        disabledRules?.let { "disabled_rules" to it.joinToString(",") },
    ).toMap()

    val correctedErrors = mutableListOf<LintError>()
    val uncorrectedErrors = mutableListOf<LintError>()

    val params = KtLint.Params(
        fileName = file.virtualFile.name,
        text = file.text,
        ruleSets = findRulesets(config.useExperimental),
        userData = userData,
        script = !file.virtualFile.name.endsWith(".kt", ignoreCase = true),
        editorConfigPath = editorConfigPath,
        debug = false,
        cb = { lintError, corrected ->
            if (corrected) {
                correctedErrors.add(lintError)
            } else {
                uncorrectedErrors.add(lintError)
            }
        },
    )

    if (format) {
        val results = KtLint.format(params)
        file.viewProvider.document?.setText(results)
    } else {
        KtLint.lint(params)
    }

    return LintResult(correctedErrors, uncorrectedErrors)
}

private fun findRulesets(experimental: Boolean) = listOfNotNull(
    // these should be loaded via ServiceLoader from the classpath + provided jar paths
    StandardRuleSetProvider().get(),
    if (experimental) ExperimentalRuleSetProvider().get() else null,
)

data class LintResult(
    val correctedErrors: List<LintError>,
    val uncorrectedErrors: List<LintError>,
)
