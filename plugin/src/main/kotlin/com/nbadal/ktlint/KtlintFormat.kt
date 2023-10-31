package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.LintError
import java.nio.file.Files
import java.nio.file.Path

internal fun ktlintLint(
    psiFile: PsiFile,
    triggeredBy: String,
) = executeKtlintFormat(psiFile, triggeredBy, false)

internal fun ktlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
) {
    val project = psiFile.project
    val document = psiFile.viewProvider.document
    PsiDocumentManager
        .getInstance(project)
        .doPostponedOperationsAndUnblockDocument(document)
    WriteCommandAction.runWriteCommandAction(project) {
        executeKtlintFormat(psiFile, triggeredBy, true)
    }
    FileDocumentManager.getInstance().saveDocument(document)
    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
    PsiDocumentManager
        .getInstance(project)
        .doPostponedOperationsAndUnblockDocument(document)
}

private fun executeKtlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
    writeFormattedCode: Boolean = false,
): List<LintError> {
    if (psiFile.virtualFile.extension !in setOf("kt", "kts")) {
        // Not a file which can be processed by ktlint
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    val virtualFileName = psiFile.virtualFile.name

    val project = psiFile.project

    println("Start ktlintFormat on file '$virtualFileName' triggered by '$triggeredBy'")

    // KtLint wants the full file path in order to search for .editorconfig files
    val filePath = psiFile.virtualFile.path

    if (filePath == "/fragment.kt") {
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    project
        .config()
        .ruleSetProviders
        .takeIf { !it.isLoaded }
        ?.let {
            KtlintNotifier
                .notifyErrorWithSettings(
                    project = project,
                    subtitle = "Error in external ruleset JAR",
                    content = project.config().ruleSetProviders.error.orEmpty(),
                )
            return EMPTY_KTLINT_FORMAT_RESULT
        }

    val baselineErrors = project.baselineErrors(filePath)
    val lintErrors = mutableListOf<LintError>()
    try {
        val formattedCode =
            project
                .config()
                .ktlintRuleEngine(psiFile.findEditorConfigDirectoryPath())
                ?.format(
                    // The psiFile may contain unsaved changes. So create a snippet based on content of the psiFile *and*
                    // with the same path as that psiFile so that the correct '.editorconfig' is picked up by ktlint.
                    Code.fromSnippet(
                        content = psiFile.text,
                        // TODO: de-comment when parameter is supported in Ktlint 1.1.0
                        // path = psiFile.virtualFile.toNioPath(),
                    ),
                ) { error, _ ->
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
                ?: return EMPTY_KTLINT_FORMAT_RESULT
                    .also { println("Could not create ktlintRuleEngine for path '$filePath'") }
        if (writeFormattedCode) {
            psiFile.viewProvider.document.setText(formattedCode)
        }
        println("Finished ktlintFormat on file '$virtualFileName' triggered by '$triggeredBy' successfully")
    } catch (pe: KtLintParseException) {
        // TODO: report to rollbar?
        println("ktlintFormat on file '$virtualFileName', KtlintParseException: " + pe.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    } catch (re: KtLintRuleException) {
        // No valid rules were passed
        println("ktlintFormat on file '$virtualFileName', KtLintRuleException: " + re.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    return lintErrors
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

private val EMPTY_KTLINT_FORMAT_RESULT = emptyList<LintError>()
