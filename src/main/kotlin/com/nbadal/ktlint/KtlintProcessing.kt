package com.nbadal.ktlint

import com.intellij.openapi.fileEditor.FileDocumentManager
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
    val editorConfigPath: String? = null

    val userData = listOfNotNull(
        "android" to config.androidMode.toString(),
        // Skip entry if empty, so we don't overwrite the .editorconfig
        config.disabledRules
            .let { if (it.isNotEmpty()) ("disabled_rules" to it.joinToString(",")) else null },
    ).toMap()

    var fileName = file.virtualFile.name
    // KtLint wants the full file path in order to search for .editorconfig files
    // Attempt to get the real file path:
    file.viewProvider.document?.let { doc ->
        FileDocumentManager.getInstance().getFile(doc)?.let { file ->
            fileName = file.path
        }
    }

    val correctedErrors = mutableListOf<LintError>()
    val uncorrectedErrors = mutableListOf<LintError>()

    val params = KtLint.Params(
        fileName = fileName,
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

    // Clear editorconfig cache. (ideally, we could do this if .editorconfig files were changed)
    KtLint.trimMemory()

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
