package com.nbadal.ktlint

import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.ruleset.experimental.ExperimentalRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider

internal fun doLint(
    file: PsiFile,
    android: Boolean = false,
    experimental: Boolean = false,
    disabledRules: List<String>? = null,
    editorConfigPath: String? = null,
): List<LintError> {
    val userData = listOfNotNull(
        "android" to android.toString(),
        disabledRules?.let { "disabled_rules" to it.joinToString(",") },
    ).toMap()

    val errors = mutableListOf<LintError>()

    val onError: (LintError, Boolean) -> Unit = { lintError, _ -> errors.add(lintError) }
    val ruleSets = findRulesets(experimental)

    KtLint.lint(
        KtLint.Params(
            fileName = file.virtualFile.name,
            text = file.text,
            ruleSets = ruleSets,
            userData = userData,
            script = !file.virtualFile.name.endsWith(".kt", ignoreCase = true),
            editorConfigPath = editorConfigPath,
            debug = false,
            cb = onError,
        )
    )

    return errors
}

private fun findRulesets(experimental: Boolean) = listOfNotNull(
    // these should be loaded via ServiceLoader from the classpath + provided jar paths
    StandardRuleSetProvider().get(),
    if (experimental) ExperimentalRuleSetProvider().get() else null,
)
