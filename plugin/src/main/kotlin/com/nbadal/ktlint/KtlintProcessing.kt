package com.nbadal.ktlint

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.applyIf
import com.nbadal.ktlint.service.loadRuleProviders
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.LintError
import java.io.File

internal fun doLint(
    file: PsiFile,
    config: KtlintConfigStorage,
    format: Boolean,
): LintResult {
    // This is a workaround for #310 -- we should figure out exactly when/why a virtualFile would be null
    if (file.virtualFile == null) {
        return emptyLintResult()
    }

    var fileName = file.virtualFile.name
    // KtLint wants the full file path in order to search for .editorconfig files
    // Attempt to get the real file path:
    file.viewProvider.document?.let { doc ->
        FileDocumentManager.getInstance().getFile(doc)?.let { file ->
            fileName = file.path
        }
    }

    if (fileName == "/fragment.kt") {
        return emptyLintResult()
    }

    // Get relative path, if possible.
    var projectRelativePath = fileName
    file.project.basePath?.let { projectRelativePath = fileName.removePrefix(it).removePrefix("/") }
    val baselineErrors =
        config.baselinePath?.let { loadBaseline(it).lintErrorsPerFile[projectRelativePath] } ?: emptyList()

    val correctedErrors = mutableListOf<LintError>()
    val uncorrectedErrors = mutableListOf<LintError>()
    val ignoredErrors = mutableListOf<LintError>()

    val ruleProviders = try {
        loadRuleProviders(config.externalJarPaths.map { File(it).toURI().toURL() })
    } catch (err: Throwable) {
        KtlintNotifier.notifyErrorWithSettings(file.project, "Error in ruleset", err.toString())
        return emptyLintResult()
    }

    val editorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE

    val engine = KtLintRuleEngine(
        editorConfigOverride = editorConfigOverride,
        ruleProviders = ruleProviders,
    )

    // Clear editorconfig cache. (ideally, we could do this if .editorconfig files were changed)
    engine.trimMemory()

    try {
        if (format) {
            val results = engine.format(
                Code.fromFile(File(fileName)),
            ) { error, corrected ->
                if (corrected) {
                    correctedErrors.add(error)
                } else if (!baselineErrors.contains(error.toCliError(corrected))) {
                    uncorrectedErrors.add(error)
                } else {
                    ignoredErrors.add(error)
                }
            }
            WriteCommandAction.runWriteCommandAction(
                file.project,
                "Format with ktlint",
                null,
                {
                    file.viewProvider.document?.apply {
                        PsiDocumentManager
                            .getInstance(file.project)
                            .doPostponedOperationsAndUnblockDocument(this)
                        setText(results)
                    }
                },
                file,
            )
        } else {
            engine.lint(
                Code.fromFile(File(fileName)),
            ) { error ->
                if (!baselineErrors.contains(error.toCliError(false))) {
                    uncorrectedErrors.add(error)
                } else {
                    ignoredErrors.add(error)
                }
            }
        }
    } catch (pe: KtLintParseException) {
        // TODO: report to rollbar?
        return emptyLintResult()
    } catch (re: KtLintRuleException) {
        // No valid rules were passed
        return emptyLintResult()
    }

    return LintResult(correctedErrors, uncorrectedErrors, ignoredErrors)
}

private fun LintError.toCliError(corrected: Boolean): KtlintCliError = KtlintCliError(
    line = this.line,
    col = this.col,
    ruleId = this.ruleId.value,
    detail = this.detail.applyIf(corrected) { "$this (cannot be auto-corrected)" },
    status = if (corrected) KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED else KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED,
)

data class LintResult(
    val correctedErrors: List<LintError>,
    val uncorrectedErrors: List<LintError>,
    val ignoredErrors: List<LintError>,
)

fun emptyLintResult() = LintResult(emptyList(), emptyList(), emptyList())
