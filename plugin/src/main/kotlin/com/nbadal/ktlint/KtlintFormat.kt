package com.nbadal.ktlint

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride.Companion.EMPTY_EDITOR_CONFIG_OVERRIDE
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.api.LintError
import java.io.File
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal fun ktlintFormat(
    psiFile: PsiFile,
    triggeredBy: String,
    writeFormattedCode: Boolean = true,
): KtlintFormatResult {
    val virtualFileName = psiFile.virtualFile.name
    println("Start ktlintFormat on file '${virtualFileName}' triggered by '$triggeredBy'")

    val project = psiFile.project
    if (!project.config().enableKtlint) {
        return EMPTY_KTLINT_FORMAT_RESULT
    }


    if (!psiFile.virtualFile.isKotlinFile()) {
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    // KtLint wants the full file path in order to search for .editorconfig files
    val filePath = psiFile.virtualFile.path

    if (filePath == "/fragment.kt") {
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    val baselineErrors = loadBaseline(project, filePath)

    val ruleProviders = try {
        loadRuleProviders(project)
    } catch (err: Throwable) {
        KtlintNotifier.notifyErrorWithSettings(project, "Error in external ruleset JAR", err.toString())
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    val correctedErrors = mutableListOf<LintError>()
    val canNotBeAutoCorrectedErrors = mutableListOf<LintError>()
    val ignoredErrors = mutableListOf<LintError>()
    try {
        // If file contains unsaved changed, those will not be picked up by the KtlintRuleEngine.
        // Add new Code factory methode to create snippet with a virtual path. The ".editorconfig" will be read from
        // this virtual path.
        // Given code snippet below which does not contain lint violations:
        //     fun foo(
        //         a: String,
        //         b: String,
        //         c: Int
        //     ) = a + b + c
        // Joining any two consecutive lines with shortcut CTRL-SHIFT-J should result in a new lint violation. But,
        // no such violation is found as the shortcut does not directly save the changes. Whenever the lines are
        // merged by deleting the newline with either backspace or delete, does result in merging the lines *and*
        // saving the changes before invoking the annotator.
        val formattedCode =
            KtLintRuleEngine(
                editorConfigOverride = EMPTY_EDITOR_CONFIG_OVERRIDE,
                ruleProviders = ruleProviders,
            ).format(Code.fromFile(File(filePath))) { error, corrected ->
                when {
                    baselineErrors.contains(error.toCliError(error.canBeAutoCorrected)) -> ignoredErrors.add(error)
                    corrected -> correctedErrors.add(error)
                    else -> canNotBeAutoCorrectedErrors.add(error)
                }
            }
        if (writeFormattedCode) {
            WriteCommandAction.runWriteCommandAction(
                project,
                "Format With KtLint",
                null,
                {
                    psiFile
                        .viewProvider
                        .document
                        ?.apply {
                            PsiDocumentManager
                                .getInstance(project)
                                .doPostponedOperationsAndUnblockDocument(this)
                            setText(formattedCode)
                            DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                        }
                },
                psiFile,
            )
        }
    } catch (pe: KtLintParseException) {
        // TODO: report to rollbar?
        println("ktlintFormat on file '${virtualFileName}', KtlintParseException: " + pe.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    } catch (re: KtLintRuleException) {
        // No valid rules were passed
        println("ktlintFormat on file '${virtualFileName}', KtLintRuleException: " + re.stackTrace)
        return EMPTY_KTLINT_FORMAT_RESULT
    }

    // TODO: remove
    println(
        """
        ktlintFormat on file '${virtualFileName}' finished:
          - correctedErrors = ${correctedErrors.size}
          - canNotBeAutoCorrectedErrors = ${canNotBeAutoCorrectedErrors.size}
          - ignoredErrors = ${ignoredErrors.size}
        """.trimIndent()
    )
    return KtlintFormatResult(canNotBeAutoCorrectedErrors, correctedErrors, ignoredErrors)
}

private fun loadBaseline(
    project: Project,
    filePath: String
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

private fun LintError.toCliError(corrected: Boolean): KtlintCliError = KtlintCliError(
    line = this.line,
    col = this.col,
    ruleId = this.ruleId.value,
    detail = this.detail.applyIf(corrected) { "$this (cannot be auto-corrected)" },
    status = if (corrected) KtlintCliError.Status.FORMAT_IS_AUTOCORRECTED else KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED,
)

data class KtlintFormatResult(
    val canNotBeAutoCorrectedErrors: List<LintError>,
    val correctedErrors: List<LintError>,
    val ignoredErrors: List<LintError>,
)

private val EMPTY_KTLINT_FORMAT_RESULT = KtlintFormatResult(emptyList(), emptyList(), emptyList())
