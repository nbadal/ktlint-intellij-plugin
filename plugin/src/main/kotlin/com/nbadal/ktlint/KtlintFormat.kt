package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.nbadal.ktlint.KtlintConfigStorage.KtlintMode.DISTRACT_FREE
import com.nbadal.ktlint.KtlintResult.Status.FILE_RELATED_ERROR
import com.nbadal.ktlint.KtlintResult.Status.NOT_STARTED
import com.nbadal.ktlint.KtlintResult.Status.PLUGIN_CONFIGURATION_ERROR
import com.nbadal.ktlint.KtlintResult.Status.SUCCESS
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.LintError
import org.ec4j.core.parser.ParseException
import java.nio.file.Files
import java.nio.file.Path

internal fun ktlintLint(
    psiFile: PsiFile,
    triggeredBy: String,
) = if (psiFile.virtualFile.isKotlinFile()) {
    executeKtlintFormat(psiFile, triggeredBy, false, force = true)
} else {
    KtlintResult(NOT_STARTED)
}

internal fun ktlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
    forceFormat: Boolean = false,
): KtlintResult {
    var ktlintResult = KtlintResult(NOT_STARTED)
    if (psiFile.virtualFile.isKotlinFile()) {
        val project = psiFile.project
        val document = psiFile.viewProvider.document
        PsiDocumentManager
            .getInstance(project)
            .doPostponedOperationsAndUnblockDocument(document)
        WriteCommandAction.runWriteCommandAction(project) {
            ktlintResult = executeKtlintFormat(psiFile, triggeredBy, true, forceFormat)
        }
        FileDocumentManager.getInstance().saveDocument(document)
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
        PsiDocumentManager
            .getInstance(project)
            .doPostponedOperationsAndUnblockDocument(document)
    }
    return ktlintResult
}

private fun executeKtlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
    writeFormattedCode: Boolean = false,
    force: Boolean = false,
): KtlintResult {
    val project = psiFile.project
    if (project.config().ktlintMode != DISTRACT_FREE && !force) {
        return KtlintResult(NOT_STARTED)
    }

    println("Start ktlintFormat on file '${psiFile.virtualFile.name}' triggered by '$triggeredBy'")

    project
        .config()
        .ruleSetProviders
        .takeIf { !it.isLoaded }
        ?.let {
            KtlintNotifier
                .notifyError(
                    project = project,
                    title = "Error in external ruleset JAR",
                    message =
                        """
                        One or more of the external rule set JAR's defined in the ktlint settings, can not be loaded.
                        Error: ${project.config().ruleSetProviders.error.orEmpty()}
                        """.trimMargin(),
                    forceSettingsDialog = true,
                )
            return KtlintResult(PLUGIN_CONFIGURATION_ERROR)
        }

    // KtLint wants the full file path in order to search for .editorconfig files
    val baselineErrors = project.baselineErrors(psiFile.virtualFile.path)

    val lintErrors = mutableListOf<LintError>()
    var fileChangedByFormat = false
    // The psiFile may contain unsaved changes. So create a snippet based on content of the psiFile *and*
    // with the same path as that psiFile so that the correct '.editorconfig' is picked up by ktlint.
    val code =
        Code.fromSnippet(
            // Get the content via the PsiDocumentManager instead of from "psiFile.text" directly. In case
            // the content of an active editor window is changed via a global find and replace, the document
            // text is updated but the Psi (and PsiFile) have not yet been changed.
            content = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!.text,
            script = psiFile.virtualFile.path.endsWith(".kts"),
            // TODO: de-comment when parameter is supported in Ktlint 1.1.0
            // path = psiFile.virtualFile.toNioPath(),
        )

    try {
        val formattedCode =
            project
                .config()
                .ktlintRuleEngine(psiFile.findEditorConfigDirectoryPath())
                ?.format(code) { error, _ ->
                    when {
                        // TODO: remove exclusion of rule "standard:filename" as this now results in false positives. When
                        //  using "Code.fromSnippet" in Ktlint 1.0.0, the filename "File.kt" or "File.kts" is being used
                        //  instead of the real name of the file. With fix in Ktlint 1.1.0 the filename will be based on
                        //  parameter "path" and the rule will no longer cause false positives.
                        error.ruleId.value == "standard:filename" -> {
                            println("Ignore rule '${error.ruleId.value}'")
                        }

                        error.isIgnoredInBaseline(baselineErrors) -> Unit
                        else -> lintErrors.add(error)
                    }
                }
                ?: return KtlintResult(FILE_RELATED_ERROR)
                    .also { println("Could not create ktlintRuleEngine for path '${psiFile.virtualFile.path}'") }
        if (writeFormattedCode && formattedCode != code.content) {
            psiFile.viewProvider.document.setText(formattedCode)
            fileChangedByFormat = true
        }
        println("Finished ktlintFormat on file '${psiFile.virtualFile.name}' triggered by '$triggeredBy' successfully")
        return KtlintResult(SUCCESS, lintErrors, fileChangedByFormat)
    } catch (ktLintParseException: KtLintParseException) {
        // Most likely the file contains a compilation error which prevents it from being parsed. The user should resolve those errors.
        // The stacktrace is excluded from the message as it would distract from resolving the error.
        KtlintNotifier.notifyWarning(
            project = project,
            title = "Parsing error",
            message =
                """
                File '${psiFile.virtualFile.path}' can not be parsed by ktlint. Please resolve all (compilation) errors first.
                Error: ${ktLintParseException.message}
                """.trimIndent(),
        )
        return KtlintResult(FILE_RELATED_ERROR)
    } catch (ktLintRuleException: KtLintRuleException) {
        KtlintNotifier.notifyError(
            project = project,
            title = "KtLintRuleException",
            message =
                """
                An error occurred in a rule. Please see stacktrace below for rule that caused the problem and contact maintainer of the
                rule when the error can be reproduced.
                ${ktLintRuleException.stackTraceToString()}
                """.trimIndent(),
        )
        return KtlintResult(FILE_RELATED_ERROR)
    } catch (parseException: ParseException) {
        KtlintNotifier.notifyError(
            project = project,
            title = "Invalid editorconfig",
            // The exception message already contains the path to the file, so don't repeat it
            message =
                """
                An error occurred while reading the '.editorconfig':
                ${parseException.message}
                """.trimIndent(),
        )
        return KtlintResult(FILE_RELATED_ERROR)
    } catch (exception: Exception) {
        KtlintNotifier.notifyError(
            project = project,
            title = "Invalid editorconfig",
            message =
                """
                An error occurred while processing file '${psiFile.virtualFile.path}':
                ${exception.stackTraceToString()}
                """.trimIndent(),
        )
        return KtlintResult(FILE_RELATED_ERROR)
    }
}

internal data class KtlintResult(
    val status: Status,
    val lintErrors: List<LintError> = emptyList(),
    val fileChangedByFormat: Boolean = false,
) {
    enum class Status {
        /**
         * Ktlint was not (yet) executed
         */
        NOT_STARTED,

        /**
         * Ktlint ran successfully.
         */
        SUCCESS,

        /**
         * Ktlint can not run due to error in plugin configuration.
         */
        PLUGIN_CONFIGURATION_ERROR,

        /**
         * Ktlint can not run due to error related to the file.
         */
        FILE_RELATED_ERROR,
    }
}

private fun LintError.isIgnoredInBaseline(baselineErrors: List<KtlintCliError>) =
    baselineErrors
        .any { baselineError ->
            baselineError.line == line &&
                baselineError.col == col &&
                baselineError.ruleId == ruleId.value &&
                baselineError.status == KtlintCliError.Status.BASELINE_IGNORED
        }

private fun PsiFile.findEditorConfigDirectoryPath(): Path? {
    var directory = virtualFile.toNioPath().parent
    while (Files.notExists(directory.resolve(".editorconfig")) && directory.parent != null) {
        directory = directory.parent
    }
    return directory
}

private fun Project.baselineErrors(filePath: String) =
    config()
        .baseline
        .lintErrorsPerFile[filePath.pathRelativeTo(basePath)]
        .orEmpty()

private fun String.pathRelativeTo(projectBasePath: String?): String =
    if (projectBasePath.isNullOrBlank()) {
        this
    } else {
        removePrefix(projectBasePath).removePrefix("/")
    }

private val EMPTY_LINT_ERRORS = emptyList<LintError>()
