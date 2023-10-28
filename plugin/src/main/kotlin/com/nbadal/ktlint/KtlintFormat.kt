package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.propertyTypes
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
    PsiDocumentManager
        .getInstance(project)
        .doPostponedOperationsAndUnblockDocument(document)
}

private fun executeKtlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
    writeFormattedCode: Boolean = false,
): KtlintFormatResult {
    val virtualFileName = psiFile.virtualFile.name
    println("Start ktlintFormat on file '$virtualFileName' triggered by '$triggeredBy'")

    val project = psiFile.project
    if (!project.config().enableKtlint) {
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    if (psiFile.virtualFile.extension !in setOf("kt", "kts")) {
        // Not a file which can be processed by ktlint
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    // KtLint wants the full file path in order to search for .editorconfig files
    val filePath = psiFile.virtualFile.path

    if (filePath == "/fragment.kt") {
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    val baselineErrors = loadBaseline(project, filePath)

    val ruleProviders =
        try {
            loadRuleProviders(project)
        } catch (err: Throwable) {
            KtlintNotifier.notifyErrorWithSettings(project, "Error in external ruleset JAR", err.toString())
            return EMPTY_KTLINT_FORMAT_RESULT
        }

    val correctedErrors = mutableListOf<LintError>()
    val canNotBeAutoCorrectedErrors = mutableListOf<LintError>()
    val ignoredErrors = mutableListOf<LintError>()
    try {
        val formattedCode =
            KtLintRuleEngine(
                editorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
                ruleProviders = ruleProviders,
                // TODO: remove when Code.fromSnippet takes a path as parameter in Ktlint 1.1.0.
                //  Drawback of this method is that it ignores property "root" in '.editorconfig' file.
                editorConfigDefaults =
                    EditorConfigDefaults.load(
                        path = psiFile.findEditorConfigDirectoryPath(),
                        propertyTypes = ruleProviders.propertyTypes(),
                    ),
            ).format(
                // The psiFile may contain unsaved changes. So create a snippet based on content of the psiFile *and*
                // with the same path as that psiFile so that the correct '.editorconfig' is picked up by ktlint.
                Code.fromSnippet(
                    content = psiFile.text,
                    // TODO: de-comment when parameter is supported in Ktlint 1.1.0
                    // path = psiFile.virtualFile.toNioPath(),
                ),
            ) { error, corrected ->
                when {
                    // TODO: remove exclusion of rule "standard:filename" as this now results in false positives. When
                    //  using "Code.fromSnippet" in Ktlint 1.0.0, the filename "File.kt" or "File.kts" is being used
                    //  instead of the real name of the file. With fix in Ktlint 1.1.0 the filename will be based on
                    //  parameter "path" and the rule will no longer cause false positives.
                    error.ruleId.value == "standard:filename" -> {
                        println("Ignore rule '${error.ruleId.value}'")
                    }
                    error.isIgnoredInBaseline(baselineErrors) -> ignoredErrors.add(error)
                    corrected -> correctedErrors.add(error)
                    else -> canNotBeAutoCorrectedErrors.add(error)
                }
            }
        if (writeFormattedCode) {
            with(psiFile.viewProvider.document) {
                setText(formattedCode)
                FileDocumentManager.getInstance().saveDocument(this)
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        }
    } catch (pe: KtLintParseException) {
        // TODO: report to rollbar?
        println("ktlintFormat on file '$virtualFileName', KtlintParseException: " + pe.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    } catch (re: KtLintRuleException) {
        // No valid rules were passed
        println("ktlintFormat on file '$virtualFileName', KtLintRuleException: " + re.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    // TODO: remove
    println(
        """
        ktlintFormat on file '$virtualFileName' finished:
          - correctedErrors = ${correctedErrors.size}
          - canNotBeAutoCorrectedErrors = ${canNotBeAutoCorrectedErrors.size}
          - ignoredErrors = ${ignoredErrors.size}
        """.trimIndent(),
    )
    return KtlintFormatResult(canNotBeAutoCorrectedErrors, correctedErrors, ignoredErrors)
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

private fun loadBaseline(
    project: Project,
    filePath: String,
) = project
    .config()
    .baselinePath
    ?.let {
        val relativeFilePath = filePath.pathRelativeTo(project.basePath)
        loadBaseline(it).lintErrorsPerFile[relativeFilePath]
    }
    ?: emptyList()

private fun String.pathRelativeTo(projectBasePath: String?): String =
    if (projectBasePath.isNullOrBlank()) {
        this
    } else {
        removePrefix(projectBasePath).removePrefix("/")
    }

data class KtlintFormatResult(
    val canNotBeAutoCorrectedErrors: List<LintError>,
    val correctedErrors: List<LintError>,
    val ignoredErrors: List<LintError>,
)

private val EMPTY_KTLINT_FORMAT_RESULT = KtlintFormatResult(emptyList(), emptyList(), emptyList())
