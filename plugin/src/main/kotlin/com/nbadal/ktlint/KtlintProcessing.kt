package com.nbadal.ktlint

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.core.api.EditorConfigOverride.Companion.plus
import com.pinterest.ktlint.core.api.KtLintParseException
import com.pinterest.ktlint.core.api.KtLintRuleException
import com.pinterest.ktlint.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.internal.loadBaseline
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import kotlin.io.path.Path

internal fun doLint(
    file: PsiFile,
    config: KtlintConfigStorage,
    format: Boolean,
): LintResult {
    val editorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE
        .applyIf(config.disabledRules.isNotEmpty()) {
            plus(
                *config.disabledRules
                    .filter { it.isNotBlank() }
                    .map { ruleId -> createRuleExecutionEditorConfigProperty(ruleId) to RuleExecution.disabled }
                    .toTypedArray(),
            )
        }
        .applyIf(config.androidMode) {
            plus(CODE_STYLE_PROPERTY to CodeStyleValue.android)
        }

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
        config.baselinePath?.let { loadBaseline(it).baselineRules?.get(projectRelativePath) } ?: emptyList()

    val correctedErrors = mutableListOf<LintError>()
    val uncorrectedErrors = mutableListOf<LintError>()
    val ignoredErrors = mutableListOf<LintError>()

    val ruleProviders = try {
        KtlintRules.findRuleProviders(config.externalJarPaths, config.useExperimental)
    } catch (err: Throwable) {
        KtlintNotifier.notifyErrorWithSettings(file.project, "Error in ruleset", err.toString())
        return emptyLintResult()
    }

    val engine = KtLintRuleEngine(
        editorConfigOverride = editorConfigOverride,
        ruleProviders = ruleProviders,
    )

    // Clear editorconfig cache. (ideally, we could do this if .editorconfig files were changed)
    engine.trimMemory()

    try {
        if (format) {
            val results = engine.format(
                file.text,
                Path(fileName),
            ) { error, corrected ->
                if (corrected) {
                    correctedErrors.add(error)
                } else if (!baselineErrors.contains(error)) {
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
                file.text,
                Path(fileName),
            ) { error ->
                if (!baselineErrors.contains(error)) {
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

data class LintResult(
    val correctedErrors: List<LintError>,
    val uncorrectedErrors: List<LintError>,
    val ignoredErrors: List<LintError>,
)

fun emptyLintResult() = LintResult(emptyList(), emptyList(), emptyList())
